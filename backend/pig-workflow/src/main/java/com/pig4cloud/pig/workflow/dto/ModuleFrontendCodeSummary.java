package com.pig4cloud.pig.workflow.dto;

import java.time.LocalDateTime;

/** 项目工作台中的模块前端代码当前版本摘要。 */
public record ModuleFrontendCodeSummary(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, int fileCount, Long uiDesignVersionId, String reviewComment,
		LocalDateTime createTime) {
}
