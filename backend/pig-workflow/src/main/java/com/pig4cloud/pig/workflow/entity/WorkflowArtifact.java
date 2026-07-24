package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流产物的稳定逻辑身份。
 */
@Data
@TableName("wf_artifact")
@EqualsAndHashCode(callSuper = true)
public class WorkflowArtifact extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long projectId;

	private Long instanceId;

	private Long moduleId;

	private String artifactCode;

	private String name;

	private String artifactType;

	private String status;

	private Long currentVersionId;

	private String notionPageId;

	@TableLogic
	private String delFlag;

}
