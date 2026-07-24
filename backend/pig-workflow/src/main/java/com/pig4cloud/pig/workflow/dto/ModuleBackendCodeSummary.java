package com.pig4cloud.pig.workflow.dto;

import java.time.LocalDateTime;

/** 项目工作台中的模块后端代码当前版本摘要。 */
public record ModuleBackendCodeSummary(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, int fileCount, String apiSummary, String reviewComment,
		LocalDateTime createTime) {
}
