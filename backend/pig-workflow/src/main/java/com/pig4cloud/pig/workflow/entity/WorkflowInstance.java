package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 工作流实例。 */
@Data
@TableName("wf_instance")
@EqualsAndHashCode(callSuper = true)
public class WorkflowInstance extends BaseEntity {
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long projectId;
	private Long definitionId;
	private String businessKey;
	private String title;
	private String status;
	private String currentNodeKey;
	private String inputJson;
	private String outputJson;
	private String startedBy;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	@TableLogic
	private String delFlag;
}
