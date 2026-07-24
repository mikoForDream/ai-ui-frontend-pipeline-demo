package com.pig4cloud.pig.workflow.dto;

import java.time.LocalDateTime;

/** 项目工作台中的模块原型当前版本摘要。 */
public record ModulePrototypeSummary(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, String reviewComment, LocalDateTime createTime) {
}
