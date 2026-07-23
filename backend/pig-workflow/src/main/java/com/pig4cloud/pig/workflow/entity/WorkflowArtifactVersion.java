package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 不可覆盖的工作流产物版本。
 */
@Data
@TableName("wf_artifact_version")
@EqualsAndHashCode(callSuper = true)
public class WorkflowArtifactVersion extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long artifactId;

	private String versionNo;

	private String sourceType;

	private String sourceUrl;

	private Long fileId;

	private String repositoryUrl;

	private String branchName;

	private String commitSha;

	private String contentJson;

	private String checksum;

	private String status;

}
