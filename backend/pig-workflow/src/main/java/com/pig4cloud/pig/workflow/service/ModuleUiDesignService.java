package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.common.file.core.FileTemplate;
import com.pig4cloud.pig.workflow.dto.ModuleUiDesignDetail;
import com.pig4cloud.pig.workflow.dto.ModuleUiDesignSummary;
import com.pig4cloud.pig.workflow.dto.UiDesignReviewRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 模块 UI 草稿生成、设计图上传、预览和人工审核。 */
@Service
@RequiredArgsConstructor
public class ModuleUiDesignService {

	private static final String BUCKET = "workflow-ui-designs";
	private static final String ARTIFACT_TYPE = "UI_DESIGN";
	private static final String GENERATOR = "RULE_BASED_UI_V1";
	private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
	private static final Set<String> REVIEW_ACTIONS = Set.of("APPROVE", "REJECT", "RETURN");

	private final ObjectMapper objectMapper;
	private final FileTemplate fileTemplate;
	private final UiDesignRenderer renderer;
	private final WorkflowProjectMapper projectMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowFeatureMapper featureMapper;
	private final WorkflowArtifactMapper artifactMapper;
	private final WorkflowArtifactVersionMapper versionMapper;

	@Transactional(rollbackFor = Exception.class)
	public ModuleUiDesignSummary generate(Long moduleId) {
		WorkflowModule module = requireEditableModule(moduleId);
		WorkflowProject project = requireProject(module.getProjectId());
		List<WorkflowFeature> features = approvedFeatures(moduleId);
		WorkflowArtifact artifact = prepareArtifact(module);
		String html = renderer.render(project, module, features);
		WorkflowArtifactVersion version = newVersion(artifact, "RULE_BASED_UI_V1", checksum(html),
				writeContent(Map.of("kind", "HTML", "generator", GENERATOR, "html", html)));
		finishPending(artifact, module, project, version);
		return summary(artifact, version);
	}

	@Transactional(rollbackFor = Exception.class)
	public ModuleUiDesignSummary upload(Long moduleId, MultipartFile file) {
		WorkflowModule module = requireEditableModule(moduleId);
		if (file == null || file.isEmpty()) {
			throw new CheckedException("请选择要上传的设计图");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new CheckedException("单个设计图不能超过 20MB");
		}
		String originalName = safeOriginalName(file.getOriginalFilename());
		String extension = extension(originalName);
		if (!IMAGE_EXTENSIONS.contains(extension)) {
			throw new CheckedException("仅支持 PNG、JPG、JPEG 和 WebP 设计图");
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
			validateImageBytes(extension, bytes);
			String contentType = imageContentType(extension);
			String objectName = module.getProjectId() + "/" + module.getModuleCode() + "/" + java.util.UUID.randomUUID()
					+ "." + extension;
			fileTemplate.putObject(BUCKET, objectName, new ByteArrayInputStream(bytes), contentType);
			WorkflowArtifact artifact = prepareArtifact(module);
			String content = writeContent(Map.of("kind", "IMAGE", "bucket", BUCKET, "objectName", objectName,
					"originalName", originalName, "contentType", contentType,
					"fileSize", bytes.length));
			WorkflowArtifactVersion version = newVersion(artifact, "USER_UPLOAD", DigestUtils.md5DigestAsHex(bytes), content);
			version.setSourceUrl("oss://" + BUCKET + "/" + objectName);
			versionMapper.updateById(version);
			finishPending(artifact, module, requireProject(module.getProjectId()), version);
			return summary(artifact, version);
		}
		catch (CheckedException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new CheckedException("设计图保存失败: " + exception.getMessage());
		}
	}

	public ModuleUiDesignDetail detail(Long versionId) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requireUiArtifact(version.getArtifactId());
		UiContent content = readContent(version);
		return new ModuleUiDesignDetail(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
				version.getStatus(), version.getSourceType(), content.kind(), content.originalName(), version.getReviewComment(),
				"HTML".equals(content.kind()) ? content.html() : null);
	}

	public InputStream openImage(Long versionId) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		UiContent content = readContent(version);
		if (!"IMAGE".equals(content.kind())) {
			throw new CheckedException("当前 UI 设计不是图片版本");
		}
		return (InputStream) fileTemplate.getObject(content.bucket(), content.objectName());
	}

	public String imageContentType(Long versionId) {
		return readContent(requireVersion(versionId)).contentType();
	}

	@Transactional(rollbackFor = Exception.class)
	public ModuleUiDesignSummary review(Long versionId, UiDesignReviewRequest request) {
		WorkflowArtifactVersion version = requireVersion(versionId);
		WorkflowArtifact artifact = requireUiArtifact(version.getArtifactId());
		if (!version.getId().equals(artifact.getCurrentVersionId())) {
			throw new CheckedException("只能审核模块的当前 UI 设计版本");
		}
		if (!"PENDING_REVIEW".equals(version.getStatus())) {
			throw new CheckedException("当前 UI 设计不在待审核状态");
		}
		String action = request.action() == null ? "" : request.action().trim().toUpperCase(Locale.ROOT);
		if (!REVIEW_ACTIONS.contains(action)) {
			throw new CheckedException("不支持的 UI 设计审核动作: " + action);
		}
		if (!"APPROVE".equals(action) && !StringUtils.hasText(request.comment())) {
			throw new CheckedException("驳回或退回 UI 设计时必须填写审核意见");
		}
		version.setStatus(switch (action) {
			case "APPROVE" -> "APPROVED";
			case "REJECT" -> "REJECTED";
			default -> "RETURNED";
		});
		version.setReviewComment(request.comment());
		versionMapper.updateById(version);
		artifact.setStatus("APPROVE".equals(action) ? "APPROVED" : "NEEDS_REVISION");
		artifactMapper.updateById(artifact);
		WorkflowModule module = requireModule(artifact.getModuleId());
		module.setStatus("APPROVE".equals(action) ? "UI_APPROVED" : "UI_REVISION");
		moduleMapper.updateById(module);
		refreshProjectStage(artifact.getProjectId());
		return summary(artifact, version);
	}

	private WorkflowModule requireEditableModule(Long moduleId) {
		WorkflowModule module = requireModule(moduleId);
		if (!Set.of("PROTOTYPE_APPROVED", "UI_REVISION").contains(module.getStatus())) {
			throw new CheckedException("模块原型尚未通过审核，或 UI 设计已经通过");
		}
		return module;
	}

	private List<WorkflowFeature> approvedFeatures(Long moduleId) {
		List<WorkflowFeature> features = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getModuleId, moduleId).orderByAsc(WorkflowFeature::getCreateTime));
		if (features.isEmpty() || features.stream().anyMatch(feature -> !"APPROVED".equals(feature.getStatus()))) {
			throw new CheckedException("模块全部功能点通过后才能设计 UI");
		}
		return features;
	}

	private WorkflowArtifact prepareArtifact(WorkflowModule module) {
		WorkflowArtifact artifact = artifactMapper.selectOne(Wrappers.<WorkflowArtifact>lambdaQuery()
			.eq(WorkflowArtifact::getProjectId, module.getProjectId()).eq(WorkflowArtifact::getModuleId, module.getId())
			.eq(WorkflowArtifact::getArtifactType, ARTIFACT_TYPE));
		if (artifact == null) {
			artifact = new WorkflowArtifact();
			artifact.setProjectId(module.getProjectId());
			artifact.setModuleId(module.getId());
			artifact.setArtifactCode("UI_" + module.getModuleCode());
			artifact.setName(module.getName() + " UI 设计");
			artifact.setArtifactType(ARTIFACT_TYPE);
			artifact.setStatus("DRAFT");
			artifactMapper.insert(artifact);
		}
		else if (artifact.getCurrentVersionId() != null) {
			WorkflowArtifactVersion current = requireVersion(artifact.getCurrentVersionId());
			if ("PENDING_REVIEW".equals(current.getStatus())) {
				throw new CheckedException("当前 UI 设计正在等待审核，不能重复提交");
			}
			if ("APPROVED".equals(current.getStatus())) {
				throw new CheckedException("当前 UI 设计已经审核通过");
			}
		}
		return artifact;
	}

	private WorkflowArtifactVersion newVersion(WorkflowArtifact artifact, String sourceType, String checksum,
			String content) {
		WorkflowArtifactVersion version = new WorkflowArtifactVersion();
		version.setArtifactId(artifact.getId());
		version.setVersionNo("V" + (versionMapper.selectCount(Wrappers.<WorkflowArtifactVersion>lambdaQuery()
			.eq(WorkflowArtifactVersion::getArtifactId, artifact.getId())) + 1));
		version.setSourceType(sourceType);
		version.setChecksum(checksum);
		version.setContentJson(content);
		version.setStatus("PENDING_REVIEW");
		versionMapper.insert(version);
		return version;
	}

	private void finishPending(WorkflowArtifact artifact, WorkflowModule module, WorkflowProject project,
			WorkflowArtifactVersion version) {
		artifact.setCurrentVersionId(version.getId());
		artifact.setStatus("REVIEWING");
		artifactMapper.updateById(artifact);
		module.setStatus("UI_REVIEW");
		moduleMapper.updateById(module);
		project.setCurrentStage("UI_REVIEW");
		projectMapper.updateById(project);
	}

	private void refreshProjectStage(Long projectId) {
		List<WorkflowModule> modules = moduleMapper.selectList(Wrappers.<WorkflowModule>lambdaQuery()
			.eq(WorkflowModule::getProjectId, projectId));
		boolean approved = !modules.isEmpty() && modules.stream().allMatch(module -> "UI_APPROVED".equals(module.getStatus()));
		WorkflowProject project = requireProject(projectId);
		project.setCurrentStage(approved ? "FRONTEND_READY" : "UI_REVIEW");
		projectMapper.updateById(project);
	}

	private String writeContent(Map<String, ?> content) {
		try {
			return objectMapper.writeValueAsString(content);
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("保存 UI 设计内容失败: " + exception.getMessage());
		}
	}

	private UiContent readContent(WorkflowArtifactVersion version) {
		try {
			JsonNode node = objectMapper.readTree(version.getContentJson());
			return new UiContent(node.path("kind").asText(), node.path("bucket").asText(null),
					node.path("objectName").asText(null), node.path("contentType").asText(null),
					node.path("originalName").asText(null), node.path("html").asText(null));
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("读取 UI 设计内容失败: " + exception.getMessage());
		}
	}

	private String checksum(String html) {
		return DigestUtils.md5DigestAsHex(html.getBytes(StandardCharsets.UTF_8));
	}

	private String safeOriginalName(String originalName) {
		if (!StringUtils.hasText(originalName)) {
			throw new CheckedException("设计图文件名不能为空");
		}
		String normalized = originalName.replace('\\', '/');
		String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		if (!StringUtils.hasText(name) || name.contains("..")) {
			throw new CheckedException("设计图文件名不合法");
		}
		return name;
	}

	private String extension(String name) {
		int separator = name.lastIndexOf('.');
		return separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);
	}

	private void validateImageBytes(String extension, byte[] bytes) {
		boolean valid = switch (extension) {
			case "png" -> startsWith(bytes, new int[] { 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
			case "jpg", "jpeg" -> startsWith(bytes, new int[] { 0xff, 0xd8, 0xff });
			case "webp" -> bytes.length >= 12 && startsWith(bytes, new int[] { 0x52, 0x49, 0x46, 0x46 })
					&& bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
			default -> false;
		};
		if (!valid) {
			throw new CheckedException("设计图内容与文件类型不匹配");
		}
	}

	private boolean startsWith(byte[] bytes, int[] signature) {
		if (bytes.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if ((bytes[index] & 0xff) != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private String imageContentType(String extension) {
		return switch (extension) {
			case "png" -> "image/png";
			case "jpg", "jpeg" -> "image/jpeg";
			case "webp" -> "image/webp";
			default -> throw new CheckedException("不支持的设计图类型");
		};
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
			throw new CheckedException("UI 设计版本不存在");
		}
		return version;
	}

	private WorkflowArtifact requireUiArtifact(Long artifactId) {
		WorkflowArtifact artifact = artifactMapper.selectById(artifactId);
		if (artifact == null || !ARTIFACT_TYPE.equals(artifact.getArtifactType())) {
			throw new CheckedException("模块 UI 设计不存在");
		}
		return artifact;
	}

	private ModuleUiDesignSummary summary(WorkflowArtifact artifact, WorkflowArtifactVersion version) {
		UiContent content = readContent(version);
		return new ModuleUiDesignSummary(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
				version.getStatus(), version.getSourceType(), content.kind(), content.originalName(), version.getReviewComment(),
				version.getCreateTime());
	}

	private record UiContent(String kind, String bucket, String objectName, String contentType, String originalName,
			String html) {
	}

}
