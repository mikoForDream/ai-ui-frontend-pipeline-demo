package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点动作流转规则。
 */
@Data
@TableName("wf_transition")
@EqualsAndHashCode(callSuper = true)
public class WorkflowTransition extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long definitionId;

	private String sourceNodeKey;

	private String targetNodeKey;

	private String action;

	private String conditionExpression;

	private Integer priority;

	private Boolean defaultTransition;

	@TableLogic
	private String delFlag;

}
