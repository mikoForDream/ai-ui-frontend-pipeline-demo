package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.BackendCodeReviewRequest;
import com.pig4cloud.pig.workflow.dto.BackendDevelopmentSpecRequest;
import com.pig4cloud.pig.workflow.service.ModuleBackendCodeService;
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
public class WorkflowBackendCodeController {
	private final ModuleBackendCodeService service;

	@PutMapping("/modules/{moduleId}/backend-spec")
	@HasPermission("workflow_backend_edit")
	public R<?> saveLogic(@PathVariable Long moduleId, @RequestBody BackendDevelopmentSpecRequest request) { return R.ok(service.saveLogic(moduleId, request)); }

	@PostMapping("/modules/{moduleId}/backend-codes/generate")
	@SysLog("生成模块后端代码")
	@HasPermission("workflow_backend_generate")
	public R<?> generate(@PathVariable Long moduleId) { return R.ok(service.generate(moduleId)); }

	@GetMapping("/backend-codes/{versionId}")
	@HasPermission("workflow_project_view")
	public R<?> detail(@PathVariable Long versionId) { return R.ok(service.detail(versionId)); }

	@GetMapping("/backend-codes/{versionId}/download")
	@HasPermission("workflow_project_view")
	public void download(@PathVariable Long versionId, HttpServletResponse response) throws Exception {
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("backend-code.zip", StandardCharsets.UTF_8).build().toString());
		response.getOutputStream().write(service.download(versionId));
	}

	@PostMapping("/backend-codes/{versionId}/reviews")
	@SysLog("审核模块后端代码")
	@HasPermission("workflow_backend_review")
	public R<?> review(@PathVariable Long versionId, @RequestBody BackendCodeReviewRequest request) { return R.ok(service.review(versionId, request)); }
}
