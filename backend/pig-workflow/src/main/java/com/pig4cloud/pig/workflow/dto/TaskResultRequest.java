package com.pig4cloud.pig.workflow.dto;

import lombok.Data;

/** 节点任务执行结果。 */
@Data
public class TaskResultRequest {
	private String outputJson;
	private String errorMessage;
}
