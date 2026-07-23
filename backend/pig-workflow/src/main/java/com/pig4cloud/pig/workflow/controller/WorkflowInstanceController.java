package com.pig4cloud.pig.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.StartWorkflowRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowExecutionLog;
import com.pig4cloud.pig.workflow.entity.WorkflowApproval;
import com.pig4cloud.pig.workflow.entity.WorkflowInstance;
import com.pig4cloud.pig.workflow.entity.WorkflowTask;
import com.pig4cloud.pig.workflow.mapper.WorkflowExecutionLogMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowApprovalMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowInstanceMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowTaskMapper;
import com.pig4cloud.pig.workflow.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 工作流实例接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/instances")
@Tag(name = "工作流实例")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowInstanceController {

	private final WorkflowInstanceMapper instanceMapper;
	private final WorkflowTaskMapper taskMapper;
	private final WorkflowExecutionLogMapper logMapper;
	private final WorkflowApprovalMapper approvalMapper;
	private final WorkflowEngineService engineService;

	@GetMapping("/page")
	@HasPermission("workflow_instance_view")
	@Operation(summary = "分页查询工作流实例")
	public R<?> page(Page<WorkflowInstance> page, WorkflowInstance query) {
		return R.ok(instanceMapper.selectPage(page, Wrappers.<WorkflowInstance>lambdaQuery()
			.eq(query.getDefinitionId() != null, WorkflowInstance::getDefinitionId, query.getDefinitionId())
			.eq(StrUtil.isNotBlank(query.getStatus()), WorkflowInstance::getStatus, query.getStatus())
			.like(StrUtil.isNotBlank(query.getTitle()), WorkflowInstance::getTitle, query.getTitle())
			.orderByDesc(WorkflowInstance::getCreateTime)));
	}

	@GetMapping("/{id}")
	@HasPermission("workflow_instance_view")
	@Operation(summary = "查询实例、任务和执行日志")
	public R<?> detail(@PathVariable Long id) {
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("instance", instanceMapper.selectById(id));
		detail.put("tasks", taskMapper.selectList(Wrappers.<WorkflowTask>lambdaQuery()
			.eq(WorkflowTask::getInstanceId, id).orderByAsc(WorkflowTask::getCreateTime)));
		detail.put("logs", logMapper.selectList(Wrappers.<WorkflowExecutionLog>lambdaQuery()
			.eq(WorkflowExecutionLog::getInstanceId, id).orderByAsc(WorkflowExecutionLog::getCreateTime)));
		detail.put("approvals", approvalMapper.selectList(Wrappers.<WorkflowApproval>lambdaQuery()
			.eq(WorkflowApproval::getInstanceId, id).orderByAsc(WorkflowApproval::getCreateTime)));
		return R.ok(detail);
	}

	@PostMapping("/start")
	@SysLog("启动工作流实例")
	@HasPermission("workflow_instance_start")
	@Operation(summary = "启动已发布的工作流")
	public R<?> start(@Valid @RequestBody StartWorkflowRequest request) {
		return R.ok(engineService.start(request));
	}

}
