package com.pig4cloud.pig.workflow.dto;

/** 可在线预览的模块原型版本。 */
public record ModulePrototypeDetail(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, String reviewComment, String html) {
}
