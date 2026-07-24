package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 从需求资料中拆分出的研发模块。 */
@Data
@TableName("wf_module")
@EqualsAndHashCode(callSuper = true)
public class WorkflowModule extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long projectId;
	private String moduleCode;
	private String name;
	private String description;
	private Integer sortOrder;
	private String status;

	@TableLogic
	private String delFlag;

}
