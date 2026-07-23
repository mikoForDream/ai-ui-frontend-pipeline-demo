package com.pig4cloud.pig.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.entity.WorkflowDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowNodeDefinition;
import com.pig4cloud.pig.workflow.entity.WorkflowTransition;
import com.pig4cloud.pig.workflow.mapper.WorkflowDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowNodeDefinitionMapper;
import com.pig4cloud.pig.workflow.mapper.WorkflowTransitionMapper;
import com.pig4cloud.pig.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/** 工作流定义接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/definitions")
@Tag(name = "工作流定义")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowDefinitionController {

	private final WorkflowDefinitionMapper definitionMapper;
	private final WorkflowNodeDefinitionMapper nodeMapper;
	private final WorkflowTransitionMapper transitionMapper;
	private final WorkflowDefinitionService definitionService;

	@GetMapping("/page")
	@HasPermission("workflow_definition_view")
	@Operation(summary = "分页查询工作流定义")
	public R<?> page(Page<WorkflowDefinition> page, WorkflowDefinition query) {
		return R.ok(definitionMapper.selectPage(page, Wrappers.<WorkflowDefinition>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), WorkflowDefinition::getName, query.getName())
			.eq(StrUtil.isNotBlank(query.getStatus()), WorkflowDefinition::getStatus, query.getStatus())
			.orderByDesc(WorkflowDefinition::getCreateTime)));
	}

	@GetMapping("/{id}")
	@HasPermission("workflow_definition_view")
	@Operation(summary = "查询工作流定义")
	public R<?> get(@PathVariable Long id) {
		return R.ok(definitionMapper.selectById(id));
	}

	@GetMapping("/{id}/nodes")
	@HasPermission("workflow_definition_view")
	@Operation(summary = "查询工作流节点")
	public R<?> nodes(@PathVariable Long id) {
		return R.ok(nodeMapper.selectList(Wrappers.<WorkflowNodeDefinition>lambdaQuery()
			.eq(WorkflowNodeDefinition::getDefinitionId, id)
			.orderByAsc(WorkflowNodeDefinition::getSortOrder)));
	}

	@GetMapping("/{id}/transitions")
	@HasPermission("workflow_definition_view")
	@Operation(summary = "查询工作流动作流转规则")
	public R<?> transitions(@PathVariable Long id) {
		return R.ok(transitionMapper.selectList(Wrappers.<WorkflowTransition>lambdaQuery()
			.eq(WorkflowTransition::getDefinitionId, id)
			.orderByAsc(WorkflowTransition::getSourceNodeKey)
			.orderByDesc(WorkflowTransition::getPriority)));
	}

	@PostMapping
	@SysLog("新增工作流定义")
	@HasPermission("workflow_definition_add")
	@Operation(summary = "新增工作流定义")
	public R<?> create(@RequestBody WorkflowDefinition definition) {
		return R.ok(definitionService.create(definition));
	}

	@PostMapping("/{id}/nodes")
	@SysLog("保存工作流节点")
	@HasPermission("workflow_definition_edit")
	@Operation(summary = "新增或修改工作流节点")
	public R<?> saveNode(@PathVariable Long id, @RequestBody WorkflowNodeDefinition node) {
		node.setDefinitionId(id);
		return R.ok(definitionService.saveNode(node));
	}

	@PostMapping("/{id}/transitions")
	@SysLog("保存工作流动作流转规则")
	@HasPermission("workflow_definition_edit")
	@Operation(summary = "新增或修改动作流转规则")
	public R<?> saveTransition(@PathVariable Long id, @RequestBody WorkflowTransition transition) {
		transition.setDefinitionId(id);
		return R.ok(definitionService.saveTransition(transition));
	}

	@DeleteMapping("/nodes/{nodeId}")
	@SysLog("删除工作流节点")
	@HasPermission("workflow_definition_edit")
	@Operation(summary = "删除工作流节点")
	public R<?> deleteNode(@PathVariable Long nodeId) {
		definitionService.deleteNode(nodeId);
		return R.ok();
	}

	@DeleteMapping("/transitions/{transitionId}")
	@SysLog("删除工作流动作流转规则")
	@HasPermission("workflow_definition_edit")
	@Operation(summary = "删除动作流转规则")
	public R<?> deleteTransition(@PathVariable Long transitionId) {
		definitionService.deleteTransition(transitionId);
		return R.ok();
	}

	@PostMapping("/{id}/publish")
	@SysLog("发布工作流定义")
	@HasPermission("workflow_definition_publish")
	@Operation(summary = "校验并发布工作流定义")
	public R<?> publish(@PathVariable Long id) {
		return R.ok(definitionService.publish(id));
	}

}
