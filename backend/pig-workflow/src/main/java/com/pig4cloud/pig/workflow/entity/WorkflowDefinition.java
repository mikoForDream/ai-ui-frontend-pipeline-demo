package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流定义。
 */
@Data
@TableName("wf_definition")
@EqualsAndHashCode(callSuper = true)
public class WorkflowDefinition extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long projectId;

	private String code;

	private String name;

	private Integer version;

	private String status;

	private String description;

	@TableLogic
	private String delFlag;

}
