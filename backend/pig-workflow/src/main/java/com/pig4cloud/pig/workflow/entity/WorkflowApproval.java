package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 人工审核决定记录。
 */
@Data
@TableName("wf_approval")
@EqualsAndHashCode(callSuper = true)
public class WorkflowApproval extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long instanceId;

	private Long taskId;

	private String approvalType;

	private String status;

	private String decision;

	private Long reviewerId;

	private String reviewerName;

	private Long candidateReviewerId;

	private Long candidateRoleId;

	private String comment;

	private Long artifactVersionId;

	private String operationKey;

	private LocalDateTime reviewedAt;

}
