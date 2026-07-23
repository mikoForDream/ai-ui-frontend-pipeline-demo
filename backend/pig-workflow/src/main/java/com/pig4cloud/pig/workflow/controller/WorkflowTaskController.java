package com.pig4cloud.pig.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.TaskActionRequest;
import com.pig4cloud.pig.workflow.dto.TaskResultRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowTask;
import com.pig4cloud.pig.workflow.mapper.WorkflowTaskMapper;
import com.pig4cloud.pig.workflow.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/** 工作流任务接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/tasks")
@Tag(name = "工作流任务")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowTaskController {

	private final WorkflowTaskMapper taskMapper;
	private final WorkflowEngineService engineService;

	@GetMapping("/page")
	@HasPermission("workflow_task_view")
	@Operation(summary = "分页查询节点任务")
	public R<?> page(Page<WorkflowTask> page, WorkflowTask query) {
		return R.ok(taskMapper.selectPage(page, Wrappers.<WorkflowTask>lambdaQuery()
			.eq(query.getInstanceId() != null, WorkflowTask::getInstanceId, query.getInstanceId())
			.eq(StrUtil.isNotBlank(query.getStatus()), WorkflowTask::getStatus, query.getStatus())
			.eq(query.getAssigneeId() != null, WorkflowTask::getAssigneeId, query.getAssigneeId())
			.orderByDesc(WorkflowTask::getCreateTime)));
	}

	@PostMapping("/{id}/actions")
	@SysLog("执行工作流任务动作")
	@HasPermission("workflow_task_execute")
	@Operation(summary = "执行完成、审核、驳回、返回、跳过、失败、重试或取消动作")
	public R<?> executeAction(@PathVariable Long id, @Valid @RequestBody TaskActionRequest request) {
		return R.ok(engineService.executeAction(id, request));
	}

	@PostMapping("/{id}/complete")
	@SysLog("完成工作流任务")
	@HasPermission("workflow_task_execute")
	@Operation(summary = "完成任务并推进流程")
	public R<?> complete(@PathVariable Long id, @RequestBody TaskResultRequest request) {
		return R.ok(engineService.completeTask(id, request));
	}

	@PostMapping("/{id}/fail")
	@SysLog("标记工作流任务失败")
	@HasPermission("workflow_task_execute")
	@Operation(summary = "标记任务及实例失败")
	public R<?> fail(@PathVariable Long id, @RequestBody TaskResultRequest request) {
		return R.ok(engineService.failTask(id, request));
	}

	@PostMapping("/{id}/retry")
	@SysLog("重试工作流任务")
	@HasPermission("workflow_task_retry")
	@Operation(summary = "重置失败任务并恢复实例")
	public R<?> retry(@PathVariable Long id) {
		return R.ok(engineService.retryTask(id));
	}

}
