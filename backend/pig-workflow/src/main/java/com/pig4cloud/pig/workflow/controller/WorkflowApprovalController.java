package com.pig4cloud.pig.workflow.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.ApprovalDecisionRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowApproval;
import com.pig4cloud.pig.workflow.mapper.WorkflowApprovalMapper;
import com.pig4cloud.pig.workflow.service.WorkflowApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/** 人工审核待办与时间线接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/approvals")
@Tag(name = "工作流人工审核")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowApprovalController {

	private final WorkflowApprovalMapper approvalMapper;
	private final WorkflowApprovalService approvalService;

	@GetMapping("/page")
	@HasPermission("workflow_approval_view")
	@Operation(summary = "分页查询审核待办和审核记录")
	public R<?> page(Page<WorkflowApproval> page, WorkflowApproval query) {
		return R.ok(approvalMapper.selectPage(page, Wrappers.<WorkflowApproval>lambdaQuery()
			.eq(query.getInstanceId() != null, WorkflowApproval::getInstanceId, query.getInstanceId())
			.eq(query.getReviewerId() != null, WorkflowApproval::getReviewerId, query.getReviewerId())
			.eq(query.getCandidateReviewerId() != null, WorkflowApproval::getCandidateReviewerId,
				query.getCandidateReviewerId())
			.eq(query.getCandidateRoleId() != null, WorkflowApproval::getCandidateRoleId, query.getCandidateRoleId())
			.eq(query.getStatus() != null, WorkflowApproval::getStatus, query.getStatus())
			.orderByDesc(WorkflowApproval::getCreateTime)));
	}

	@PostMapping("/{id}/claim")
	@SysLog("领取工作流审核待办")
	@HasPermission("workflow_approval_claim")
	@Operation(summary = "领取审核待办")
	public R<?> claim(@PathVariable Long id) {
		return R.ok(approvalService.claim(id));
	}

	@PostMapping("/{id}/decisions")
	@SysLog("提交工作流审核决定")
	@HasPermission("workflow_approval_decide")
	@Operation(summary = "提交审核通过、驳回或退回决定")
	public R<?> decide(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
		return R.ok(approvalService.decide(id, request));
	}

}
