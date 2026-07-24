package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.dto.UiDesignReviewRequest;
import com.pig4cloud.pig.workflow.service.ModuleUiDesignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 模块 UI 草稿、设计图上传、预览和人工审核。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow")
@Tag(name = "研发项目模块 UI 设计")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowUiDesignController {

	private final ModuleUiDesignService uiDesignService;

	@PostMapping("/modules/{moduleId}/ui-designs/generate")
	@SysLog("生成模块 UI 草稿")
	@HasPermission("workflow_ui_generate")
	@Operation(summary = "根据已通过原型生成模块 UI 草稿")
	public R<?> generate(@PathVariable Long moduleId) {
		return R.ok(uiDesignService.generate(moduleId));
	}

	@PostMapping(value = "/modules/{moduleId}/ui-designs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SysLog("上传模块 UI 设计图")
	@HasPermission("workflow_ui_upload")
	@Operation(summary = "上传用户提供的模块 UI 设计图")
	public R<?> upload(@PathVariable Long moduleId, @RequestPart("file") MultipartFile file) {
		return R.ok(uiDesignService.upload(moduleId, file));
	}

	@GetMapping("/ui-designs/{versionId}")
	@HasPermission("workflow_project_view")
	@Operation(summary = "查询模块 UI 设计版本")
	public R<?> detail(@PathVariable Long versionId) {
		return R.ok(uiDesignService.detail(versionId));
	}

	@GetMapping("/ui-designs/{versionId}/content")
	@HasPermission("workflow_project_view")
	@Operation(summary = "读取用户上传的 UI 设计图")
	public void content(@PathVariable Long versionId, HttpServletResponse response) throws Exception {
		response.setContentType(uiDesignService.imageContentType(versionId));
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("ui-design",
				StandardCharsets.UTF_8).build().toString());
		try (InputStream input = uiDesignService.openImage(versionId)) {
			StreamUtils.copy(input, response.getOutputStream());
		}
	}

	@PostMapping("/ui-designs/{versionId}/reviews")
	@SysLog("审核模块 UI 设计")
	@HasPermission("workflow_ui_review")
	@Operation(summary = "通过、驳回或退回模块 UI 设计")
	public R<?> review(@PathVariable Long versionId, @RequestBody UiDesignReviewRequest request) {
		return R.ok(uiDesignService.review(versionId, request));
	}

}
