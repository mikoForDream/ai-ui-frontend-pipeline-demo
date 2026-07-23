package com.pig4cloud.pig.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 人工审核决定请求。 */
@Data
public class ApprovalDecisionRequest {

	@NotBlank
	private String decision;

	@NotBlank
	private String operationKey;

	private String comment;

	private String outputJson;

}
