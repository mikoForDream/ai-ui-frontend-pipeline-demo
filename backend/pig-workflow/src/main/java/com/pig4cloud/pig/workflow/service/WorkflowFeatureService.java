package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.workflow.dto.FeatureReviewRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.mapper.WorkflowFeatureMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowModuleMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 功能点编辑、审核和项目阶段推进。 */
@Service
@RequiredArgsConstructor
public class WorkflowFeatureService {

	private static final Set<String> REVIEW_ACTIONS = Set.of("APPROVE", "REJECT", "RETURN");

	private final WorkflowFeatureMapper featureMapper;
	private final WorkflowModuleMapper moduleMapper;
	private final WorkflowProjectMapper projectMapper;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowFeature update(Long id, WorkflowFeature request) {
		WorkflowFeature feature = requireFeature(id);
		if ("APPROVED".equals(feature.getStatus())) {
			throw new CheckedException("已通过的功能点不能直接修改，请在后续版本中变更");
		}
		if (!StringUtils.hasText(request.getName())) {
			throw new CheckedException("功能点名称不能为空");
		}
		feature.setName(request.getName().trim());
		feature.setDescription(request.getDescription());
		feature.setAcceptanceCriteria(request.getAcceptanceCriteria());
		feature.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "MEDIUM");
		feature.setStatus("PENDING_REVIEW");
		feature.setReviewComment(null);
		feature.setVersion(feature.getVersion() == null ? 1 : feature.getVersion() + 1);
		featureMapper.updateById(feature);
		refreshProgress(feature.getProjectId(), feature.getModuleId());
		return feature;
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowFeature review(Long id, FeatureReviewRequest request) {
		WorkflowFeature feature = requireFeature(id);
		String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
		if (!REVIEW_ACTIONS.contains(action)) {
			throw new CheckedException("不支持的功能点审核动作: " + action);
		}
		if ("APPROVED".equals(feature.getStatus())) {
			throw new CheckedException("功能点已经审核通过");
		}
		if (("REJECT".equals(action) || "RETURN".equals(action)) && !StringUtils.hasText(request.getComment())) {
			throw new CheckedException("驳回或退回时必须填写审核意见");
		}
		feature.setStatus(switch (action) {
			case "APPROVE" -> "APPROVED";
			case "REJECT" -> "REJECTED";
			default -> "DRAFT";
		});
		feature.setReviewComment(request.getComment());
		featureMapper.updateById(feature);
		refreshProgress(feature.getProjectId(), feature.getModuleId());
		return feature;
	}

	private void refreshProgress(Long projectId, Long moduleId) {
		List<WorkflowFeature> moduleFeatures = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getModuleId, moduleId));
		boolean moduleApproved = !moduleFeatures.isEmpty()
				&& moduleFeatures.stream().allMatch(feature -> "APPROVED".equals(feature.getStatus()));
		WorkflowModule module = moduleMapper.selectById(moduleId);
		module.setStatus(moduleApproved ? "REQUIREMENT_APPROVED" : "REQUIREMENT_REVIEW");
		moduleMapper.updateById(module);

		List<WorkflowFeature> projectFeatures = featureMapper.selectList(Wrappers.<WorkflowFeature>lambdaQuery()
			.eq(WorkflowFeature::getProjectId, projectId));
		boolean projectApproved = !projectFeatures.isEmpty()
				&& projectFeatures.stream().allMatch(feature -> "APPROVED".equals(feature.getStatus()));
		WorkflowProject project = projectMapper.selectById(projectId);
		project.setCurrentStage(projectApproved ? "PROTOTYPE_READY" : "FEATURE_REVIEW");
		projectMapper.updateById(project);
	}

	private WorkflowFeature requireFeature(Long id) {
		WorkflowFeature feature = featureMapper.selectById(id);
		if (feature == null) {
			throw new CheckedException("功能点不存在");
		}
		return feature;
	}

}
