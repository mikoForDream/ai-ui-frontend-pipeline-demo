package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 节点执行任务。 */
@Data
@TableName("wf_task")
@EqualsAndHashCode(callSuper = true)
public class WorkflowTask extends BaseEntity {
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long instanceId;
	private Long nodeDefinitionId;
	private String nodeKey;
	private String nodeName;
	private String taskType;
	private String status;
	private Long assigneeId;
	private String inputJson;
	private String outputJson;
	private Integer retryCount;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private String errorMessage;
	@TableLogic
	private String delFlag;
}
