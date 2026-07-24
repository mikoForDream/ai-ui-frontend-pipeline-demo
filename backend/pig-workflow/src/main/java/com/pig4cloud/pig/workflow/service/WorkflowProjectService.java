package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.WorkflowMaterialSummary;
import com.pig4cloud.pig.workflow.dto.ModulePrototypeSummary;
import com.pig4cloud.pig.workflow.dto.ModuleUiDesignSummary;
import com.pig4cloud.pig.workflow.dto.ModuleFrontendCodeSummary;
import com.pig4cloud.pig.workflow.dto.WorkflowProjectWorkspace;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifact;
import com.pig4cloud.pig.workflow.entity.WorkflowArtifactVersion;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowMaterial;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.entity.WorkflowProductSpec;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowArtifactVersionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowFeatureMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowMaterialMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowModuleMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProductSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/** 研发项目及工作台聚合服务。 */
@Service
@RequiredArgsConstructor
public class WorkflowProjectService {

	private final WorkflowProjectMapper projectMapper;
	private final WorkflowMaterialMapper materialMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowFeatureMapper featureMapper;
	private final WorkflowArtifactMapper artifactMapper;
	private final WorkflowArtifactVersionMapper artifactVersionMapper;
	private final WorkflowProductSpecMapper productSpecMapper;
	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowProject create(WorkflowProject project) {
		if (!StringUtils.hasText(project.getProjectCode()) || !StringUtils.hasText(project.getName())) {
			throw new CheckedException("项目编码和名称不能为空");
		}
		String code = project.getProjectCode().trim().toUpperCase(Locale.ROOT);
		Long count = projectMapper.selectCount(Wrappers.<WorkflowProject>lambdaQuery()
			.eq(WorkflowProject::getProjectCode, code));
		if (count > 0) {
			throw new CheckedException("项目编码已存在");
		}
		project.setProjectCode(code);
		project.setName(project.getName().trim());
		project.setStatus("ACTIVE");
		project.setCurrentStage("MATERIAL_COLLECTION");
		projectMapper.insert(project);
		return project;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowProject update(Long id, WorkflowProject request) {
		WorkflowProject project = requireProject(id);
		if (StringUtils.hasText(request.getName())) {
			project.setName(request.getName().trim());
		}
		project.setDescription(request.getDescription());
		project.setRepositoryUrl(request.getRepositoryUrl());
		project.setDefaultBranch(request.getDefaultBranch());
		project.setFrontendPath(request.getFrontendPath());
		project.setBackendPath(request.getBackendPath());
		project.setTechStack(request.getTechStack());
		projectMapper.updateById(project);
		return project;
	}

	public WorkflowProjectWorkspace workspace(Long projectId) {
		WorkflowProject project = requireProject(projectId);
		List<WorkflowMaterialSummary> materials = materialMapper
			.selectList(Wrappers.<WorkflowMaterial>lambdaQuery()
				.eq(WorkflowMaterial::getProjectId, projectId)
				.orderByDesc(WorkflowMaterial::getCreateTime))
			.stream()
			.map(WorkflowMaterialSummary::from)
			.toList();
		List<WorkflowModule> modules = moduleMapper.selectList(Wrappers.<WorkflowModule>lambdaQuery()
			.eq(WorkflowModule::getProjectId, projectId)
			.orderByAsc(WorkflowModule::getSortOrder));
		List<WorkflowFeature> features = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getProjectId, projectId)
			.orderByAsc(WorkflowFeature::getModuleId)
			.orderByAsc(WorkflowFeature::getCreateTime));
		List<ModulePrototypeSummary> prototypes = artifactMapper.selectList(Wrappers.<WorkflowArtifact>lambdaQuery()
			.eq(WorkflowArtifact::getProjectId, projectId)
			.eq(WorkflowArtifact::getArtifactType, "PROTOTYPE")
			.isNotNull(WorkflowArtifact::getCurrentVersionId))
			.stream()
			.map(this::prototypeSummary)
			.filter(java.util.Objects::nonNull)
			.toList();
		List<ModuleUiDesignSummary> uiDesigns = artifactMapper.selectList(Wrappers.<WorkflowArtifact>lambdaQuery()
			.eq(WorkflowArtifact::getProjectId, projectId)
			.eq(WorkflowArtifact::getArtifactType, "UI_DESIGN")
			.isNotNull(WorkflowArtifact::getCurrentVersionId))
			.stream()
			.map(this::uiDesignSummary)
			.filter(java.util.Objects::nonNull)
			.toList();
		List<ModuleFrontendCodeSummary> frontendCodes = artifactMapper.selectList(Wrappers.<WorkflowArtifact>lambdaQuery()
			.eq(WorkflowArtifact::getProjectId, projectId).eq(WorkflowArtifact::getArtifactType, "FRONTEND_CODE")
			.isNotNull(WorkflowArtifact::getCurrentVersionId)).stream().map(this::frontendCodeSummary)
			.filter(java.util.Objects::nonNull).toList();
		WorkflowProductSpec frozenSpec = productSpecMapper.selectOne(Wrappers.<WorkflowProductSpec>lambdaQuery()
			.eq(WorkflowProductSpec::getProjectId, projectId)
			.eq(WorkflowProductSpec::getStatus, "FROZEN")
			.orderByDesc(WorkflowProductSpec::getCreateTime)
			.last("LIMIT 1"));
		return new WorkflowProjectWorkspace(project, materials, modules, features, prototypes, uiDesigns, frontendCodes,
				frozenSpec == null ? null : frozenSpec.getVersionNo());
	}

	private ModuleUiDesignSummary uiDesignSummary(WorkflowArtifact artifact) {
		WorkflowArtifactVersion version = artifactVersionMapper.selectById(artifact.getCurrentVersionId());
		if (version == null) {
			return null;
		}
		try {
			JsonNode content = objectMapper.readTree(version.getContentJson());
			return new ModuleUiDesignSummary(artifact.getId(), artifact.getModuleId(), version.getId(),
					version.getVersionNo(), version.getStatus(), version.getSourceType(), content.path("kind").asText(),
					content.path("originalName").asText(null), version.getReviewComment(), version.getCreateTime());
		}
		catch (JsonProcessingException exception) {
			throw new CheckedException("读取 UI 设计摘要失败: " + exception.getMessage());
		}
	}

	private ModulePrototypeSummary prototypeSummary(WorkflowArtifact artifact) {
		WorkflowArtifactVersion version = artifactVersionMapper.selectById(artifact.getCurrentVersionId());
		if (version == null) {
			return null;
		}
		return new ModulePrototypeSummary(artifact.getId(), artifact.getModuleId(), version.getId(),
				version.getVersionNo(), version.getStatus(), version.getSourceType(), version.getReviewComment(),
				version.getCreateTime());
	}

	private ModuleFrontendCodeSummary frontendCodeSummary(WorkflowArtifact artifact) {
		WorkflowArtifactVersion version = artifactVersionMapper.selectById(artifact.getCurrentVersionId());
		if (version == null) return null;
		try {
			JsonNode content = objectMapper.readTree(version.getContentJson());
			return new ModuleFrontendCodeSummary(artifact.getId(), artifact.getModuleId(), version.getId(), version.getVersionNo(),
					version.getStatus(), content.path("generator").asText(), content.path("files").size(),
					content.path("uiDesignVersionId").asLong(), version.getReviewComment(), version.getCreateTime());
		} catch (JsonProcessingException exception) { throw new CheckedException("读取前端代码摘要失败: " + exception.getMessage()); }
	}

	public WorkflowProject requireProject(Long id) {
		WorkflowProject project = projectMapper.selectById(id);
		if (project == null) {
			throw new CheckedException("研发项目不存在");
		}
		return project;
	}

}
