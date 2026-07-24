package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.FrontendCodeReviewRequest;
import com.pig4cloud.pig.workflow.dto.FrontendDevelopmentSpecRequest;
import com.pig4cloud.pig.workflow.service.ModuleFrontendCodeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow")
public class WorkflowFrontendCodeController {
	private final ModuleFrontendCodeService service;

	@PutMapping("/modules/{moduleId}/frontend-spec")
	@HasPermission("workflow_frontend_edit")
	public R<?> saveLogic(@PathVariable Long moduleId, @RequestBody FrontendDevelopmentSpecRequest request) { return R.ok(service.saveLogic(moduleId, request)); }

	@PostMapping("/modules/{moduleId}/frontend-codes/generate")
	@SysLog("生成模块前端代码")
	@HasPermission("workflow_frontend_generate")
	public R<?> generate(@PathVariable Long moduleId) { return R.ok(service.generate(moduleId)); }

	@GetMapping("/frontend-codes/{versionId}")
	@HasPermission("workflow_project_view")
	public R<?> detail(@PathVariable Long versionId) { return R.ok(service.detail(versionId)); }

	@GetMapping("/frontend-codes/{versionId}/download")
	@HasPermission("workflow_project_view")
	public void download(@PathVariable Long versionId, HttpServletResponse response) throws Exception {
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("frontend-code.zip", StandardCharsets.UTF_8).build().toString());
		response.getOutputStream().write(service.download(versionId));
	}

	@PostMapping("/frontend-codes/{versionId}/reviews")
	@SysLog("审核模块前端代码")
	@HasPermission("workflow_frontend_review")
	public R<?> review(@PathVariable Long versionId, @RequestBody FrontendCodeReviewRequest request) { return R.ok(service.review(versionId, request)); }
}
