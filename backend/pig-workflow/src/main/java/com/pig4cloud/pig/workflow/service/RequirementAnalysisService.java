package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.RequirementAnalysisResult;
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

/** 从已解析资料生成待人工审核的模块和功能点草稿。 */
@Service
@RequiredArgsConstructor
public class RequirementAnalysisService {

	private final WorkflowProjectService projectService;
	private final WorkflowAiGenerationService aiGenerationService;
	private final WorkflowProjectMapper projectMapper;
	private final WorkflowMaterialMapper materialMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowFeatureMapper featureMapper;

	@Transactional(rollbackFor = Exception.class)
	public RequirementAnalysisResult analyze(Long projectId) {
		WorkflowProject project = projectService.requireProject(projectId);
		Long existing = featureMapper.selectCount(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getProjectId, projectId));
		if (existing > 0) {
			throw new CheckedException("项目已经生成过功能点，请先审核或编辑现有结果");
		}
		List<String> documents = materialMapper.selectList(Wrappers.<WorkflowMaterial>lambdaQuery()
			.eq(WorkflowMaterial::getProjectId, projectId)
			.eq(WorkflowMaterial::getParseStatus, "PARSED"))
			.stream()
			.map(WorkflowMaterial::getExtractedText)
			.filter(StringUtils::hasText)
			.toList();
		if (documents.isEmpty()) {
			throw new CheckedException("没有可分析的已解析资料");
		}

		List<WorkflowAiGenerationService.AiDraftModule> drafts = aiGenerationService
				.analyzeRequirements(project, documents).modules();
		int featureIndex = 1;
		int moduleIndex = 1;
		for (WorkflowAiGenerationService.AiDraftModule draft : drafts) {
			WorkflowModule module = new WorkflowModule();
			module.setProjectId(projectId);
			module.setModuleCode("MODULE_" + String.format("%03d", moduleIndex));
			module.setName(draft.name());
			module.setDescription(draft.description());
			module.setSortOrder(moduleIndex * 10);
			module.setStatus("REQUIREMENT_REVIEW");
			moduleMapper.insert(module);
			for (WorkflowAiGenerationService.AiDraftFeature draftFeature : draft.features()) {
				WorkflowFeature feature = new WorkflowFeature();
				feature.setProjectId(projectId);
				feature.setModuleId(module.getId());
				feature.setFeatureCode("FEAT_" + String.format("%03d", featureIndex++));
				feature.setName(draftFeature.name());
				feature.setDescription(draftFeature.description());
				feature.setAcceptanceCriteria(draftFeature.acceptanceCriteria());
				feature.setPriority(draftFeature.priority());
				feature.setStatus("PENDING_REVIEW");
				feature.setVersion(1);
				featureMapper.insert(feature);
			}
			moduleIndex++;
		}
		project.setCurrentStage("FEATURE_REVIEW");
		projectMapper.updateById(project);
		return new RequirementAnalysisResult(drafts.size(), featureIndex - 1, WorkflowAiGenerationService.GENERATOR);
	}

}
