package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.WorkflowMaterialSummary;
import com.pig4cloud.pig.workflow.dto.WorkflowProjectWorkspace;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowMaterial;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.mapper.WorkflowFeatureMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowMaterialMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowModuleMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
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
		return new WorkflowProjectWorkspace(project, materials, modules, features);
	}

	public WorkflowProject requireProject(Long id) {
		WorkflowProject project = projectMapper.selectById(id);
		if (project == null) {
			throw new CheckedException("研发项目不存在");
		}
		return project;
	}

}
