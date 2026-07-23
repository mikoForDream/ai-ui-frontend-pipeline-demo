package com.pig4cloud.pig.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 统一任务动作请求。
 */
@Data
public class TaskActionRequest {

	@NotBlank
	private String action;

	private String outputJson;

	private String errorMessage;

}
