package com.pig4cloud.pig.workflow.dto;

import java.time.LocalDateTime;

/** 项目工作台中的模块 UI 设计当前版本摘要。 */
public record ModuleUiDesignSummary(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String sourceType, String contentKind, String originalName, String reviewComment,
		LocalDateTime createTime) {
}
