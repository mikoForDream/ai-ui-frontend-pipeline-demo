package com.pig4cloud.pig.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 启动工作流请求。 */
@Data
public class StartWorkflowRequest {
	@NotNull
	private Long definitionId;
	@NotBlank
	private String businessKey;
	@NotBlank
	private String title;
	private String inputJson;
}
