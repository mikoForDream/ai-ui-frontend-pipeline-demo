package com.pig4cloud.pig.workflow.dto;

import com.pig4cloud.pig.workflow.entity.WorkflowMaterial;

import java.time.LocalDateTime;

/** 项目工作台使用的资料摘要，避免返回完整解析文本。 */
public record WorkflowMaterialSummary(Long id, String originalName, String contentType, String extension,
		Long fileSize, String checksum, String parseStatus, int extractedLength, String parseMessage,
		LocalDateTime createTime) {

	public static WorkflowMaterialSummary from(WorkflowMaterial material) {
		String text = material.getExtractedText();
		return new WorkflowMaterialSummary(material.getId(), material.getOriginalName(), material.getContentType(),
				material.getExtension(), material.getFileSize(), material.getChecksum(), material.getParseStatus(),
				text == null ? 0 : text.length(), material.getParseMessage(), material.getCreateTime());
	}

}
