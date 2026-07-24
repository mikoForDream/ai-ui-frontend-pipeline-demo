package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.ModulePrototypeDetail;
import com.pig4cloud.pig.workflow.dto.ModulePrototypeSummary;
import com.pig4cloud.pig.workflow.dto.PrototypeReviewRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifact;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifactVersion;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProductSpec;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactVersionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowFeatureMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowModuleMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProductSpecMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 按模块生成、版本化和审核交互 HTML 原型。 */
@Service
@RequiredArgsConstructor
public class ModulePrototypeService {

	private static final String ARTIFACT_TYPE = "PROTOTYPE";
	private static final String GENERATOR = WorkflowAiGenerationService.GENERATOR;
	private static final Set<String> REVIEW_ACTIONS = Set.of("APPROVE", "REJECT", "RETURN");

	private final ObjectMapper objectMapper;
	private final WorkflowProjectMapper projectMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowFeatureMapper featureMapper;
	private final WorkflowArtifactMapper artifactMapper;
	private final WorkflowArtifactVersionMapper versionMapper;
	private final WorkflowProductSpecMapper productSpecMapper;
	private final WorkflowAiGenerationService aiGenerationService;

	@Transactional(rollbackFor = Exception.class)
	public ModulePrototypeSummary generate(Long moduleId) {
		WorkflowModule module = requireModule(moduleId);
		if (!Set.of("REQUIREMENT_APPROVED", "PROTOTYPE_REVISION").contains(module.getStatus())) {
			throw new CheckedException("模块需求尚未通过审核，或原型已经通过");
		}
		List<WorkflowFeature> features = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getModuleId, moduleId)
			.orderByAsc(WorkflowFeature::getCreateTime));
		if (features.isEmpty() || features.stream().anyMatch(feature -> !"APPROVED".equals(feature.getStatus()))) {
			throw new CheckedException("模块全部功能点通过后才能生成原型");
		}

		WorkflowProject project = requireProject(module.getProjectId());
		freezeRequirements(project);
		WorkflowArtifact artifact = findArtifact(module);
		if (artifact == null) {
			artifact = new WorkflowArtifact();
			artifact.setProjectId(module.getProjectId());
			artifact.setModuleId(moduleId);
			artifact.setArtifactCode("PROTOTYPE_" + module.getModuleCode());
			artifact.setName(module.getName() + "交互原型");
			artifact.setArtifactType(ARTIFACT_TYPE);
			artifact.setStatus("DRAFT");
			artifactMapper.insert(artifact);
		}
		else if (artifact.getCurrentVersionId() != null) {
			WorkflowArtifactVersion current = versionMapper.selectById(artifact.getCurrentVersionId());
			if (current != null && "PENDING_REVIEW".equals(current.getStatus())) {
				throw new CheckedException("当前原型正在等待审核，不能重复生成");
			}
			if (current != null && "APPROVED".equals(current.getStatus())) {
				throw new CheckedException("当前原型已经审核通过");
			}
		}

		String html = aiGenerationService.generatePrototype(project, module, features, currentReviewComment(artifact));
		WorkflowArtifactVersion version = new WorkflowArtifactVersion();
		version.setArtifactId(artifact.getId());
		version.setVersionNo("V" + (versionMapper.selectCount(Wrappers.<WorkflowArtifactVersion>lambdaQuery()
			.eq(WorkflowArtifactVersion::getArtifactId, artifact.getId())) + 1));
		version.setSourceType(GENERATOR);
		version.setContentJson(writePrototypeContent(html));
		version.setChecksum(DigestUtils.md5DigestAsHex(html.getBytes(StandardCharsets.UTF_8)));
		version.setStatus("PENDING_REVIEW");
		versionMapper.insert(version);

		artifact.setCurrentVersionId(version.getId());
		artifact.setStatus("REVIEWING");
		artifactMapper.updateById(artifact);
		module.setStatus("PROTOTYPE_REVIEW");
		moduleMapper.updateById(module);
		project.setCurrentStage("PROTOTYPE_REVIEW");
		projectMapper.updateById(project);
		return summary(artifact, version);
	}

	public ModulePrototypeDetail detail(Long versionId) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requirePrototypeArtifact(version.getArtifactId());
		return new ModulePrototypeDetail(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
				version.getStatus(), version.getSourceType(), version.getReviewComment(), readHtml(version));
	}

	@Transactional(rollbackFor = Exception.class)
	public ModulePrototypeSummary review(Long versionId, PrototypeReviewRequest request) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requirePrototypeArtifact(version.getArtifactId());
		if (!version.getId().equals(artifact.getCurrentVersionId())) {
			throw new CheckedException("只能审核模块的当前原型版本");
		}
		if (!"PENDING_REVIEW".equals(version.getStatus())) {
			throw new CheckedException("当前原型版本不在待审核状态");
		}
		String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
		if (!REVIEW_ACTIONS.contains(action)) {
			throw new CheckedException("不支持的原型审核动作: " + action);
		}
		if (!"APPROVE".equals(action) && !StringUtils.hasText(request.comment())) {
			throw new CheckedException("驳回或退回原型时必须填写审核意见");
		}

		String versionStatus = switch (action) {
			case "APPROVE" -> "APPROVED";
			case "REJECT" -> "REJECTED";
			default -> "RETURNED";
		};
		version.setStatus(versionStatus);
		version.setReviewComment(request.comment());
		versionMapper.updateById(version);
		artifact.setStatus("APPROVE".equals(action) ? "APPROVED" : "NEEDS_REVISION");
		artifactMapper.updateById(artifact);

		WorkflowModule module = requireModule(artifact.getModuleId());
		module.setStatus("APPROVE".equals(action) ? "PROTOTYPE_APPROVED" : "PROTOTYPE_REVISION");
		moduleMapper.updateById(module);
		refreshProjectStage(artifact.getProjectId());
		return summary(artifact, version);
	}

	private void freezeRequirements(WorkflowProject project) {
		Long notApproved = featureMapper.selectCount(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getProjectId, project.getId())
			.ne(WorkflowFeature::getStatus, "APPROVED"));
		if (notApproved > 0) {
			throw new CheckedException("项目仍有未通过的功能点，不能冻结产品规格");
		}
		WorkflowProductSpec frozen = productSpecMapper.selectOne(Wrappers.<WorkflowProductSpec>lambdaQuery()
			.eq(WorkflowProductSpec::getProjectId, project.getId())
			.eq(WorkflowProductSpec::getStatus, "FROZEN")
			.orderByDesc(WorkflowProductSpec::getCreateTime)
			.last("LIMIT 1"));
		if (frozen != null) {
			return;
		}
		List<WorkflowModule> modules = moduleMapper.selectList(Wrappers.<WorkflowModule>lambdaQuery()
			.eq(WorkflowModule::getProjectId, project.getId())
			.orderByAsc(WorkflowModule::getSortOrder));
		List<WorkflowFeature> features = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getProjectId, project.getId())
			.orderByAsc(WorkflowFeature::getModuleId)
			.orderByAsc(WorkflowFeature::getCreateTime));
		Map<String, Object> spec = new LinkedHashMap<>();
		spec.put("schemaVersion", "1.0");
		spec.put("projectCode", project.getProjectCode());
		spec.put("projectName", project.getName());
		List<Map<String, Object>> moduleSpecs = new ArrayList<>();
		for (WorkflowModule module : modules) {
			Map<String, Object> moduleSpec = new LinkedHashMap<>();
			moduleSpec.put("moduleCode", module.getModuleCode());
			moduleSpec.put("name", module.getName());
			moduleSpec.put("features", features.stream().filter(item -> module.getId().equals(item.getModuleId()))
				.map(this::featureSpec).toList());
			moduleSpecs.add(moduleSpec);
		}
		spec.put("modules", moduleSpecs);
		try {
			WorkflowProductSpec productSpec = new WorkflowProductSpec();
			productSpec.setProjectId(project.getId());
			productSpec.setSchemaVersion("1.0");
			productSpec.setVersionNo("REQ-V1");
			productSpec.setSpecJson(objectMapper.writeValueAsString(spec));
			productSpec.setStatus("FROZEN");
			productSpec.setFrozenAt(LocalDateTime.now());
			productSpecMapper.insert(productSpec);
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("冻结产品规格失败: " + exception.getMessage());
		}
	}

	private Map<String, Object> featureSpec(WorkflowFeature feature) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("featureCode", feature.getFeatureCode());
		result.put("name", feature.getName());
		result.put("description", feature.getDescription());
		result.put("acceptanceCriteria", feature.getAcceptanceCriteria());
		result.put("priority", feature.getPriority());
		result.put("version", feature.getVersion());
		return result;
	}

	private void refreshProjectStage(Long projectId) {
		List<WorkflowModule> modules = moduleMapper.selectList(Wrappers.<WorkflowModule>lambdaQuery()
			.eq(WorkflowModule::getProjectId, projectId));
		boolean approved = !modules.isEmpty()
			&& modules.stream().allMatch(module -> "PROTOTYPE_APPROVED".equals(module.getStatus()));
		WorkflowProject project = requireProject(projectId);
		project.setCurrentStage(approved ? "UI_READY" : "PROTOTYPE_REVIEW");
		projectMapper.updateById(project);
	}

	private WorkflowArtifact findArtifact(WorkflowModule module) {
		return artifactMapper.selectOne(Wrappers.<WorkflowArtifact>lambdaQuery()
			.eq(WorkflowArtifact::getProjectId, module.getProjectId())
			.eq(WorkflowArtifact::getModuleId, module.getId())
			.eq(WorkflowArtifact::getArtifactType, ARTIFACT_TYPE));
	}

	private String currentReviewComment(WorkflowArtifact artifact) {
		if (artifact == null || artifact.getCurrentVersionId() == null) return null;
		WorkflowArtifactVersion version = versionMapper.selectById(artifact.getCurrentVersionId());
		return version == null ? null : version.getReviewComment();
	}

	private WorkflowModule requireModule(Long moduleId) {
		WorkflowModule module = moduleMapper.selectById(moduleId);
		if (module == null) {
			throw new CheckedException("研发模块不存在");
		}
		return module;
	}

	private WorkflowProject requireProject(Long projectId) {
		WorkflowProject project = projectMapper.selectById(projectId);
		if (project == null) {
			throw new CheckedException("研发项目不存在");
		}
		return project;
	}

	private WorkflowArtifactVersion requireVersion(Long versionId) {
		WorkflowArtifactVersion version = versionMapper.selectById(versionId);
		if (version == null) {
			throw new CheckedException("原型版本不存在");
		}
		return version;
	}

	private WorkflowArtifact requirePrototypeArtifact(Long artifactId) {
		WorkflowArtifact artifact = artifactMapper.selectById(artifactId);
		if (artifact == null || !ARTIFACT_TYPE.equals(artifact.getArtifactType())) {
			throw new CheckedException("模块原型不存在");
		}
		return artifact;
	}

	private ModulePrototypeSummary summary(WorkflowArtifact artifact, WorkflowArtifactVersion version) {
		return new ModulePrototypeSummary(artifact.getId(), artifact.getModuleId(), version.getId(),
				version.getVersionNo(), version.getStatus(), version.getSourceType(), version.getReviewComment(),
				version.getCreateTime());
	}

	private String writePrototypeContent(String html) {
		try {
			return objectMapper.writeValueAsString(Map.of("format", "html", "generator", GENERATOR, "html", html));
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("保存原型内容失败: " + exception.getMessage());
		}
	}

	private String readHtml(WorkflowArtifactVersion version) {
		try {
			JsonNode content = objectMapper.readTree(version.getContentJson());
			return content.path("html").asText();
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("读取原型内容失败: " + exception.getMessage());
		}
	}

	String renderHtml(WorkflowProject project, WorkflowModule module, List<WorkflowFeature> features) {
		StringBuilder nav = new StringBuilder();
		StringBuilder panels = new StringBuilder();
		for (int index = 0; index < features.size(); index++) {
			WorkflowFeature feature = features.get(index);
			String active = index == 0 ? " active" : "";
			nav.append("<button class=\"nav-item").append(active).append("\" data-target=\"feature-")
				.append(index).append("\"><span>").append(escape(feature.getFeatureCode())).append("</span>")
				.append(escape(feature.getName())).append("</button>");
			panels.append("<section id=\"feature-").append(index).append("\" class=\"feature-panel").append(active)
				.append("\"><div class=\"eyebrow\">").append(escape(feature.getFeatureCode())).append(" · ")
				.append(escape(feature.getPriority())).append("</div><h2>").append(escape(feature.getName()))
				.append("</h2><p class=\"description\">").append(escape(feature.getDescription())).append("</p>")
				.append("<div class=\"work-area\"><label>业务输入</label><input placeholder=\"输入本功能所需信息\">")
				.append("<button class=\"primary action\">执行操作</button><div class=\"feedback\" role=\"status\"></div></div>")
				.append("<div class=\"acceptance\"><strong>验收标准</strong><p>")
				.append(escape(feature.getAcceptanceCriteria())).append("</p></div></section>");
		}
		return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
			+ "<title>" + escape(module.getName()) + "交互原型</title><style>"
			+ "*{box-sizing:border-box}body{margin:0;font-family:Arial,'Microsoft YaHei',sans-serif;color:#17202a;background:#f4f6f7}"
			+ ".shell{min-height:100vh;display:grid;grid-template-columns:250px 1fr}.sidebar{background:#182126;color:#fff;padding:24px 16px}.brand{font-size:13px;color:#9fc5b5;margin-bottom:8px}.sidebar h1{font-size:20px;margin:0 8px 24px}.nav-item{width:100%;border:0;background:transparent;color:#c9d3d7;text-align:left;padding:12px;border-radius:6px;cursor:pointer;margin-bottom:6px}.nav-item span{display:block;font-size:11px;color:#7d929b;margin-bottom:4px}.nav-item.active{background:#2f6b58;color:#fff}.main{padding:28px 34px}.topbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:28px}.project{font-size:13px;color:#68757b}.badge{background:#e7f0ed;color:#285d4c;padding:7px 10px;border-radius:4px;font-size:12px}.feature-panel{display:none;max-width:850px}.feature-panel.active{display:block}.eyebrow{font-size:12px;color:#2f6b58;font-weight:bold}.feature-panel h2{font-size:28px;margin:8px 0 10px}.description{color:#56646a;line-height:1.7}.work-area{background:#fff;border:1px solid #d9e0e2;padding:24px;margin:24px 0;border-radius:6px}.work-area label{display:block;font-size:13px;font-weight:bold;margin-bottom:8px}.work-area input{width:100%;padding:11px;border:1px solid #b9c4c8;border-radius:4px;margin-bottom:14px}.primary{border:0;background:#2f6b58;color:#fff;padding:10px 16px;border-radius:4px;cursor:pointer}.feedback{min-height:20px;margin-top:14px;color:#2f6b58}.acceptance{border-left:4px solid #c08b2c;background:#fff8e8;padding:14px 18px}.acceptance p{margin:7px 0 0;line-height:1.6}@media(max-width:720px){.shell{grid-template-columns:1fr}.sidebar{padding:16px}.sidebar h1{margin-bottom:12px}.main{padding:22px}.topbar{align-items:flex-start;gap:12px}.feature-panel h2{font-size:23px}}"
			+ "</style></head><body><div class=\"shell\"><aside class=\"sidebar\"><div class=\"brand\">"
			+ escape(project.getName()) + "</div><h1>" + escape(module.getName()) + "</h1><nav>" + nav
			+ "</nav></aside><main class=\"main\"><header class=\"topbar\"><div class=\"project\">模块交互原型 · "
			+ features.size() + " 个功能点</div><span class=\"badge\">待业务审核</span></header>" + panels
			+ "</main></div><script>document.querySelectorAll('.nav-item').forEach(function(button){button.addEventListener('click',function(){document.querySelectorAll('.nav-item,.feature-panel').forEach(function(item){item.classList.remove('active')});button.classList.add('active');document.getElementById(button.dataset.target).classList.add('active')})});document.querySelectorAll('.action').forEach(function(button){button.addEventListener('click',function(){button.parentElement.querySelector('.feedback').textContent='操作已完成，原型交互反馈正常。'})});</script></body></html>";
	}

	private String escape(String value) {
		return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
	}

}
