package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 前后端共享的版本化产品规格。
 */
@Data
@TableName("wf_product_spec")
@EqualsAndHashCode(callSuper = true)
public class WorkflowProductSpec extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long projectId;

	private Long instanceId;

	private Long artifactVersionId;

	private String schemaVersion;

	private String versionNo;

	private String specJson;

	private String status;

	private LocalDateTime frozenAt;

	@TableLogic
	private String delFlag;

}
