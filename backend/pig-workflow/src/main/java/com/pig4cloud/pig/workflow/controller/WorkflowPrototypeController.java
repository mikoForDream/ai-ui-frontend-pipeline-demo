package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.PrototypeReviewRequest;
import com.pig4cloud.pig.workflow.service.ModulePrototypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 模块交互原型生成、预览与人工审核。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow")
@Tag(name = "研发项目模块原型")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowPrototypeController {

	private final ModulePrototypeService prototypeService;

	@PostMapping("/modules/{moduleId}/prototypes")
	@SysLog("生成模块交互原型")
	@HasPermission("workflow_prototype_generate")
	@Operation(summary = "根据已审核功能点生成模块原型新版本")
	public R<?> generate(@PathVariable Long moduleId) {
		return R.ok(prototypeService.generate(moduleId));
	}

	@GetMapping("/prototypes/{versionId}")
	@HasPermission("workflow_project_view")
	@Operation(summary = "查询可在线预览的模块原型版本")
	public R<?> detail(@PathVariable Long versionId) {
		return R.ok(prototypeService.detail(versionId));
	}

	@PostMapping("/prototypes/{versionId}/reviews")
	@SysLog("审核模块交互原型")
	@HasPermission("workflow_prototype_review")
	@Operation(summary = "通过、驳回或退回模块原型")
	public R<?> review(@PathVariable Long versionId, @RequestBody PrototypeReviewRequest request) {
		return R.ok(prototypeService.review(versionId, request));
	}

}
