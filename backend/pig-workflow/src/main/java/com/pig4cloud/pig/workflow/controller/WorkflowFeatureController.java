package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.FeatureReviewRequest;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.service.WorkflowFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 产品功能点编辑与人工审核。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/features")
@Tag(name = "研发项目功能点")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowFeatureController {

	private final WorkflowFeatureService featureService;

	@PutMapping("/{id}")
	@SysLog("修改项目功能点")
	@HasPermission("workflow_feature_edit")
	@Operation(summary = "修改并重新提交功能点审核")
	public R<?> update(@PathVariable Long id, @RequestBody WorkflowFeature feature) {
		return R.ok(featureService.update(id, feature));
	}

	@PostMapping("/{id}/reviews")
	@SysLog("审核项目功能点")
	@HasPermission("workflow_feature_review")
	@Operation(summary = "通过、驳回或退回功能点")
	public R<?> review(@PathVariable Long id, @RequestBody FeatureReviewRequest request) {
		return R.ok(featureService.review(id, request));
	}

}
