package com.pig4cloud.pig.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.workflow.constant.WorkflowAction;
import com.pig4cloud.pig.workflow.dto.ApprovalDecisionRequest;
import com.pig4cloud.pig.workflow.dto.TaskActionRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowApproval;
import com.pig4cloud.pig.workflow.mapper.WorkflowApprovalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/** 人工审核领取与决策服务。 */
@Service
@RequiredArgsConstructor
public class WorkflowApprovalService {

	private static final String PENDING = "PENDING";
	private static final String CLAIMED = "CLAIMED";
	private static final String DECIDED = "DECIDED";
	private static final Set<String> DECISIONS = Set.of(
		WorkflowAction.APPROVE, WorkflowAction.REJECT, WorkflowAction.RETURN);

	private final WorkflowApprovalMapper approvalMapper;
	private final WorkflowEngineService engineService;

	@Transactional(rollbackFor = Exception.class)
	public WorkflowApproval claim(Long approvalId) {
		WorkflowApproval approval = requireApproval(approvalId);
		Long userId = SecurityUtils.getUser().getId();
		String username = SecurityUtils.getUser().getUsername();
		if (CLAIMED.equals(approval.getStatus()) && userId.equals(approval.getReviewerId())) {
			return approval;
		}
		if (!PENDING.equals(approval.getStatus())) {
			throw new CheckedException("审核待办已被处理");
		}
		if (approval.getCandidateReviewerId() != null && !userId.equals(approval.getCandidateReviewerId())) {
			throw new CheckedException("当前用户不在该审核待办的候选人范围内");
		}
		if (approval.getCandidateRoleId() != null && !SecurityUtils.getRoles().contains(approval.getCandidateRoleId())) {
			throw new CheckedException("当前用户不具备该审核待办要求的角色");
		}
		int changed = approvalMapper.update(null, Wrappers.<WorkflowApproval>lambdaUpdate()
			.eq(WorkflowApproval::getId, approvalId)
			.eq(WorkflowApproval::getStatus, PENDING)
			.isNull(WorkflowApproval::getReviewerId)
			.set(WorkflowApproval::getStatus, CLAIMED)
			.set(WorkflowApproval::getReviewerId, userId)
			.set(WorkflowApproval::getReviewerName, username));
		if (changed != 1) {
			throw new CheckedException("审核待办已被其他用户领取，请刷新后重试");
		}
		return approvalMapper.selectById(approvalId);
	}

	@Transactional(rollbackFor = Exception.class)
	public WorkflowApproval decide(Long approvalId, ApprovalDecisionRequest request) {
		WorkflowApproval approval = requireApproval(approvalId);
		String decision = request.getDecision().trim().toUpperCase(Locale.ROOT);
		if (!DECISIONS.contains(decision)) {
			throw new CheckedException("人工审核仅支持 APPROVE、REJECT 或 RETURN");
		}
		if (DECIDED.equals(approval.getStatus())) {
			if (request.getOperationKey().equals(approval.getOperationKey()) && decision.equals(approval.getDecision())) {
				return approval;
			}
			throw new CheckedException("审核已经完成，不能重复提交其他决定");
		}
		Long userId = SecurityUtils.getUser().getId();
		String username = SecurityUtils.getUser().getUsername();
		if (approval.getReviewerId() != null && !userId.equals(approval.getReviewerId())) {
			throw new CheckedException("该审核待办已由其他用户领取");
		}
		int changed = approvalMapper.update(null, Wrappers.<WorkflowApproval>lambdaUpdate()
			.eq(WorkflowApproval::getId, approvalId)
			.in(WorkflowApproval::getStatus, PENDING, CLAIMED)
			.and(wrapper -> wrapper.isNull(WorkflowApproval::getReviewerId)
				.or().eq(WorkflowApproval::getReviewerId, userId))
			.set(WorkflowApproval::getStatus, DECIDED)
			.set(WorkflowApproval::getDecision, decision)
			.set(WorkflowApproval::getReviewerId, userId)
			.set(WorkflowApproval::getReviewerName, username)
			.set(WorkflowApproval::getComment, request.getComment())
			.set(WorkflowApproval::getOperationKey, request.getOperationKey())
			.set(WorkflowApproval::getReviewedAt, LocalDateTime.now()));
		if (changed != 1) {
			throw new CheckedException("审核状态已变化，请刷新后重试");
		}

		TaskActionRequest action = new TaskActionRequest();
		action.setAction(decision);
		action.setOutputJson(request.getOutputJson());
		engineService.executeAction(approval.getTaskId(), action);
		return approvalMapper.selectById(approvalId);
	}

	private WorkflowApproval requireApproval(Long approvalId) {
		WorkflowApproval approval = approvalMapper.selectById(approvalId);
		if (approval == null) {
			throw new CheckedException("审核记录不存在");
		}
		return approval;
	}

}
