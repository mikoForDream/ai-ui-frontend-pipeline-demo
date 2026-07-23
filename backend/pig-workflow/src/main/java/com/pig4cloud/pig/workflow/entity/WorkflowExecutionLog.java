package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工作流执行审计日志。 */
@Data
@TableName("wf_execution_log")
@EqualsAndHashCode(callSuper = true)
public class WorkflowExecutionLog extends BaseEntity {
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long instanceId;
	private Long taskId;
	private String nodeKey;
	private String eventType;
	private String status;
	private String requestJson;
	private String responseJson;
	private String errorMessage;
	private Long durationMs;
}
