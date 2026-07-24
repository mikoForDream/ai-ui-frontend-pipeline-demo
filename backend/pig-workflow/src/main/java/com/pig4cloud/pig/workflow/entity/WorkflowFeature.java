package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 可独立审核和追踪的产品功能点。 */
@Data
@TableName("wf_feature")
@EqualsAndHashCode(callSuper = true)
public class WorkflowFeature extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long projectId;
	private Long moduleId;
	private String featureCode;
	private String name;
	private String description;
	private String acceptanceCriteria;
	private String priority;
	private String status;
	private String reviewComment;
	private Integer version;

	@TableLogic
	private String delFlag;

}
