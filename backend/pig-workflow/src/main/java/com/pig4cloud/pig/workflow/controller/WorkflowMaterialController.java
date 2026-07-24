package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.entity.WorkflowMaterial;
import com.pig4cloud.pig.workflow.service.WorkflowMaterialService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 项目资料上传、解析与下载。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow")
@Tag(name = "AI 研发项目资料")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowMaterialController {

	private final WorkflowMaterialService materialService;

	@PostMapping(value = "/projects/{projectId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SysLog("上传项目资料")
	@HasPermission("workflow_material_upload")
	@Operation(summary = "上传并解析项目资料")
	public R<?> upload(@PathVariable Long projectId, @RequestPart("file") MultipartFile file) {
		return R.ok(materialService.upload(projectId, file));
	}

	@PostMapping("/materials/{id}/parse")
	@SysLog("重新解析项目资料")
	@HasPermission("workflow_material_analyze")
	@Operation(summary = "重新解析项目资料")
	public R<?> parse(@PathVariable Long id) {
		return R.ok(materialService.parse(id));
	}

	@GetMapping("/materials/{id}/download")
	@HasPermission("workflow_project_view")
	@Operation(summary = "下载项目原始资料")
	public void download(@PathVariable Long id, HttpServletResponse response) throws Exception {
		WorkflowMaterial material = materialService.requireMaterial(id);
		response.setContentType(material.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE
				: material.getContentType());
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
			.filename(material.getOriginalName(), StandardCharsets.UTF_8)
			.build()
			.toString());
		try (InputStream input = materialService.open(id)) {
			StreamUtils.copy(input, response.getOutputStream());
		}
	}

}
