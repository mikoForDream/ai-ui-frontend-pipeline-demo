package com.pig4cloud.pig.workflow.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import com.pig4cloud.pig.workflow.mapper.WorkflowProjectMapper;
import com.pig4cloud.pig.workflow.service.RequirementAnalysisService;
import com.pig4cloud.pig.workflow.service.WorkflowProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 研发项目和需求分析入口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/projects")
@Tag(name = "AI 研发项目")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowProjectController {

	private final WorkflowProjectMapper projectMapper;
	private final WorkflowProjectService projectService;
	private final RequirementAnalysisService analysisService;

	@GetMapping("/page")
	@HasPermission("workflow_project_view")
	@Operation(summary = "分页查询研发项目")
	public R<?> page(Page<WorkflowProject> page, WorkflowProject query) {
		return R.ok(projectMapper.selectPage(page, Wrappers.<WorkflowProject>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), WorkflowProject::getName, query.getName())
			.eq(StrUtil.isNotBlank(query.getStatus()), WorkflowProject::getStatus, query.getStatus())
			.orderByDesc(WorkflowProject::getCreateTime)));
	}

	@GetMapping("/{id}/workspace")
	@HasPermission("workflow_project_view")
	@Operation(summary = "查询研发项目工作台")
	public R<?> workspace(@PathVariable Long id) {
		return R.ok(projectService.workspace(id));
	}

	@PostMapping
	@SysLog("新增 AI 研发项目")
	@HasPermission("workflow_project_edit")
	@Operation(summary = "新增研发项目")
	public R<?> create(@RequestBody WorkflowProject project) {
		return R.ok(projectService.create(project));
	}

	@PutMapping("/{id}")
	@SysLog("修改 AI 研发项目")
	@HasPermission("workflow_project_edit")
	@Operation(summary = "修改研发项目")
	public R<?> update(@PathVariable Long id, @RequestBody WorkflowProject project) {
		return R.ok(projectService.update(id, project));
	}

	@PostMapping("/{id}/analysis")
	@SysLog("分析项目需求资料")
	@HasPermission("workflow_material_analyze")
	@Operation(summary = "根据已解析资料生成模块和功能点草稿")
	public R<?> analyze(@PathVariable Long id) {
		return R.ok(analysisService.analyze(id));
	}

}
