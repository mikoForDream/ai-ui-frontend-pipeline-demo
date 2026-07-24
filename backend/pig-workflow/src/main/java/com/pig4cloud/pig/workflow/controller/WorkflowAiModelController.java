package com.pig4cloud.pig.workflow.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.workflow.ai.AiModelGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuration metadata only; the API key is never serialized. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/ai-model")
@Tag(name = "AI 模型服务")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class WorkflowAiModelController {

	private final AiModelGateway gateway;

	@GetMapping("/status")
	@HasPermission("workflow_project_view")
	@Operation(summary = "查询 AI 模型配置状态")
	public R<?> status() {
		return R.ok(gateway.status());
	}

	@PostMapping("/connectivity")
	@SysLog("检测 AI 模型服务连接")
	@HasPermission("workflow_material_analyze")
	@Operation(summary = "执行一次最小结构化生成以检测模型连接")
	public R<?> connectivity() {
		return R.ok(gateway.checkConnectivity());
	}
}
