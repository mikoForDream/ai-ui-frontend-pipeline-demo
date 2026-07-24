package com.pig4cloud.pig.workflow.dto;

/** 可在线预览的模块 UI 设计版本。 */
public record ModuleUiDesignDetail(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String sourceType, String contentKind, String originalName, String reviewComment, String html) {
}
