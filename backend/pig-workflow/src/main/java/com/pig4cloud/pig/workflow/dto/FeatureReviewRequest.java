package com.pig4cloud.pig.workflow.dto;

import lombok.Data;

/** 功能点人工审核请求。 */
@Data
public class FeatureReviewRequest {

	private String action;
	private String comment;

}
