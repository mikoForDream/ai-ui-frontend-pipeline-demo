package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 项目上传资料及解析结果。 */
@Data
@TableName("wf_material")
@EqualsAndHashCode(callSuper = true)
public class WorkflowMaterial extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private Long projectId;
	private String originalName;
	private String objectName;
	private String bucketName;
	private String contentType;
	private String extension;
	private Long fileSize;
	private String checksum;
	private String parseStatus;
	private String extractedText;
	private String parseMessage;

	@TableLogic
	private String delFlag;

}
