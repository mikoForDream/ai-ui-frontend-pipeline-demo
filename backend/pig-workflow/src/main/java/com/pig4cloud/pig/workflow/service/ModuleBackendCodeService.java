package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.BackendCodeReviewRequest;
import com.pig4cloud.pig.workflow.dto.BackendDevelopmentSpecRequest;
import com.pig4cloud.pig.workflow.dto.GeneratedCodeFile;
import com.pig4cloud.pig.workflow.dto.ModuleBackendCodeDetail;
import com.pig4cloud.pig.workflow.dto.ModuleBackendCodeSummary;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifact;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifactVersion;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactVersionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowFeatureMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowModuleMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 模块 Java 后端代码生成、版本化、下载和人工审核。 */
@Service
@RequiredArgsConstructor
public class ModuleBackendCodeService {

	private static final String ARTIFACT_TYPE = "BACKEND_CODE";
	private static final String GENERATOR = WorkflowAiGenerationService.GENERATOR;
	private static final int MAX_LOGIC_LENGTH = 10_000;
	private static final Set<String> REVIEW_ACTIONS = Set.of("APPROVE", "REJECT", "RETURN");

	private final ObjectMapper objectMapper;
	private final WorkflowAiGenerationService aiGenerationService;
	private final WorkflowProjectMapper projectMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowFeatureMapper featureMapper;
	private final WorkflowArtifactMapper artifactMapper;
	private final WorkflowArtifactVersionMapper versionMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowModule saveLogic(Long moduleId, BackendDevelopmentSpecRequest request) {
		WorkflowModule module = requireEditableModule(moduleId);
		String logic = request == null ? null : request.logic();
		if (logic != null && logic.length() > MAX_LOGIC_LENGTH) {
			throw new CheckedException("模块后端实现逻辑不能超过 10000 个字符");
		}
		module.setBackendLogic(StringUtils.hasText(logic) ? logic.trim() : null);
		moduleMapper.updateById(module);
		return module;
	}

	@Transactional(rollbackFor = Exception.class)
	public ModuleBackendCodeSummary generate(Long moduleId) {
		WorkflowModule module = requireEditableModule(moduleId);
		WorkflowProject project = requireProject(module.getProjectId());
		List<WorkflowFeature> features = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
				.eq(WorkflowFeature::getModuleId, moduleId).orderByAsc(WorkflowFeature::getCreateTime));
		if (features.isEmpty() || features.stream().anyMatch(feature -> !"APPROVED".equals(feature.getStatus()))) {
			throw new CheckedException("模块全部功能点通过后才能生成后端代码");
		}
		WorkflowArtifact artifact = prepareArtifact(module);
		WorkflowAiGenerationService.GeneratedBackend generated = aiGenerationService.generateBackend(project, module,
				features, currentReviewComment(artifact));
		String content = writeContent(Map.of("generator", GENERATOR, "backendLogic",
				module.getBackendLogic() == null ? "" : module.getBackendLogic(), "apiSummary", generated.apiSummary(),
				"files", generated.files()));
		WorkflowArtifactVersion version = new WorkflowArtifactVersion();
		version.setArtifactId(artifact.getId());
		version.setVersionNo("V" + (versionMapper.selectCount(Wrappers.<WorkflowArtifactVersion>lambdaQuery()
				.eq(WorkflowArtifactVersion::getArtifactId, artifact.getId())) + 1));
		version.setSourceType(GENERATOR);
		version.setContentJson(content);
		version.setChecksum(DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)));
		version.setStatus("PENDING_REVIEW");
		versionMapper.insert(version);
		artifact.setCurrentVersionId(version.getId());
		artifact.setStatus("REVIEWING");
		artifactMapper.updateById(artifact);
		module.setStatus("BACKEND_REVIEW");
		moduleMapper.updateById(module);
		project.setCurrentStage("BACKEND_REVIEW");
		projectMapper.updateById(project);
		return summary(artifact, version);
	}

	public ModuleBackendCodeDetail detail(Long versionId) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requireBackendArtifact(version.getArtifactId());
		BackendContent content = readContent(version);
		return new ModuleBackendCodeDetail(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
				version.getStatus(), content.generator(), content.backendLogic(), content.apiSummary(),
				version.getReviewComment(), content.files());
	}

	public byte[] download(Long versionId) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		requireBackendArtifact(version.getArtifactId());
		BackendContent content = readContent(version);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
			for (GeneratedCodeFile file : content.files()) {
				if (!ModuleFrontendCodeService.isSafeArchivePath(file.path())) {
					throw new CheckedException("代码包包含不安全的文件路径");
				}
				zip.putNextEntry(new ZipEntry(file.path().replace('\\', '/')));
				zip.write(file.content().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
			zip.finish();
			return output.toByteArray();
		}
		catch (CheckedException exception) { throw exception; }
		catch (Exception exception) { throw new CheckedException("生成后端代码包失败"); }
	}

	@Transactional(rollbackFor = Exception.class)
	public ModuleBackendCodeSummary review(Long versionId, BackendCodeReviewRequest request) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requireBackendArtifact(version.getArtifactId());
		if (!version.getId().equals(artifact.getCurrentVersionId())) throw new CheckedException("只能审核模块的当前后端代码版本");
		if (!"PENDING_REVIEW".equals(version.getStatus())) throw new CheckedException("当前后端代码不在待审核状态");
		String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
		if (!REVIEW_ACTIONS.contains(action)) throw new CheckedException("不支持的后端代码审核动作: " + action);
		if (!"APPROVE".equals(action) && (request == null || !StringUtils.hasText(request.comment()))) {
			throw new CheckedException("驳回或退回后端代码时必须填写审核意见");
		}
		version.setStatus(switch (action) { case "APPROVE" -> "APPROVED"; case "REJECT" -> "REJECTED"; default -> "RETURNED"; });
		version.setReviewComment(request == null ? null : request.comment());
		versionMapper.updateById(version);
		artifact.setStatus("APPROVE".equals(action) ? "APPROVED" : "NEEDS_REVISION");
		artifactMapper.updateById(artifact);
		WorkflowModule module = requireModule(artifact.getModuleId());
		module.setStatus("APPROVE".equals(action) ? "BACKEND_APPROVED" : "BACKEND_REVISION");
		moduleMapper.updateById(module);
		refreshProjectStage(artifact.getProjectId());
		return summary(artifact, version);
	}

	private WorkflowModule requireEditableModule(Long moduleId) {
		WorkflowModule module = requireModule(moduleId);
		if (!Set.of("FRONTEND_APPROVED", "BACKEND_READY", "BACKEND_REVISION").contains(module.getStatus())) {
			throw new CheckedException("项目尚未进入后端开发阶段，或当前后端代码已提交审核/通过");
		}
		return module;
	}

	private WorkflowArtifact prepareArtifact(WorkflowModule module) {
		WorkflowArtifact artifact = artifactMapper.selectOne(Wrappers.<WorkflowArtifact>lambdaQuery()
				.eq(WorkflowArtifact::getProjectId, module.getProjectId()).eq(WorkflowArtifact::getModuleId, module.getId())
				.eq(WorkflowArtifact::getArtifactType, ARTIFACT_TYPE));
		if (artifact == null) {
			artifact = new WorkflowArtifact(); artifact.setProjectId(module.getProjectId()); artifact.setModuleId(module.getId());
			artifact.setArtifactCode("BE_" + module.getModuleCode()); artifact.setName(module.getName() + "后端代码");
			artifact.setArtifactType(ARTIFACT_TYPE); artifact.setStatus("DRAFT"); artifactMapper.insert(artifact);
		}
		else if (artifact.getCurrentVersionId() != null) {
			WorkflowArtifactVersion current = requireVersion(artifact.getCurrentVersionId());
			if ("PENDING_REVIEW".equals(current.getStatus())) throw new CheckedException("当前后端代码正在等待审核，不能重复生成");
			if ("APPROVED".equals(current.getStatus())) throw new CheckedException("当前后端代码已经审核通过");
		}
		return artifact;
	}

	private void refreshProjectStage(Long projectId) {
		List<WorkflowModule> modules = moduleMapper.selectList(Wrappers.<WorkflowModule>lambdaQuery().eq(WorkflowModule::getProjectId, projectId));
		boolean approved = !modules.isEmpty() && modules.stream().allMatch(module -> "BACKEND_APPROVED".equals(module.getStatus()));
		WorkflowProject project = requireProject(projectId);
		project.setCurrentStage(approved ? "INTEGRATION" : "BACKEND_DEVELOPMENT");
		projectMapper.updateById(project);
	}

	private String currentReviewComment(WorkflowArtifact artifact) {
		if (artifact == null || artifact.getCurrentVersionId() == null) return null;
		WorkflowArtifactVersion version = versionMapper.selectById(artifact.getCurrentVersionId());
		return version == null ? null : version.getReviewComment();
	}

	private String writeContent(Map<String, ?> content) {
		try { return objectMapper.writeValueAsString(content); }
		catch (JsonProcessingException exception) { throw new CheckedException("保存后端代码失败"); }
	}

	private BackendContent readContent(WorkflowArtifactVersion version) {
		try {
			JsonNode node = objectMapper.readTree(version.getContentJson());
			return new BackendContent(node.path("generator").asText(), node.path("backendLogic").asText(),
					node.path("apiSummary").asText(), objectMapper.convertValue(node.path("files"), new TypeReference<List<GeneratedCodeFile>>() { }));
		}
		catch (IllegalArgumentException | JsonProcessingException exception) { throw new CheckedException("读取后端代码失败"); }
	}

	private ModuleBackendCodeSummary summary(WorkflowArtifact artifact, WorkflowArtifactVersion version) {
		BackendContent content = readContent(version);
		return new ModuleBackendCodeSummary(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
				version.getStatus(), content.generator(), content.files().size(), content.apiSummary(), version.getReviewComment(), version.getCreateTime());
	}

	private WorkflowModule requireModule(Long moduleId) { WorkflowModule module = moduleMapper.selectById(moduleId); if (module == null) throw new CheckedException("研发模块不存在"); return module; }
	private WorkflowProject requireProject(Long projectId) { WorkflowProject project = projectMapper.selectById(projectId); if (project == null) throw new CheckedException("研发项目不存在"); return project; }
	private WorkflowArtifactVersion requireVersion(Long versionId) { WorkflowArtifactVersion version = versionMapper.selectById(versionId); if (version == null) throw new CheckedException("后端代码版本不存在"); return version; }
	private WorkflowArtifact requireBackendArtifact(Long artifactId) { WorkflowArtifact artifact = artifactMapper.selectById(artifactId); if (artifact == null || !ARTIFACT_TYPE.equals(artifact.getArtifactType())) throw new CheckedException("模块后端代码不存在"); return artifact; }
	private record BackendContent(String generator, String backendLogic, String apiSummary, List<GeneratedCodeFile> files) { }
}
