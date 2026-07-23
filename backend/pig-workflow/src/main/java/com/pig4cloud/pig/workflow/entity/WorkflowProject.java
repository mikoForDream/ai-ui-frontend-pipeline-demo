package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流研发项目。
 */
@Data
@TableName("wf_project")
@EqualsAndHashCode(callSuper = true)
public class WorkflowProject extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private String projectCode;

	private String name;

	private String description;

	private String status;

	private String repositoryUrl;

	private String defaultBranch;

	private String frontendPath;

	private String backendPath;

	private String techStack;

	private String currentSpecVersion;

	private String notionPageId;

	@TableLogic
	private String delFlag;

}
