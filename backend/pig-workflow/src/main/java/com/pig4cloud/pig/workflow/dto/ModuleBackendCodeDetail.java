package com.pig4cloud.pig.workflow.dto;

import java.util.List;

/** 模块后端代码版本详情。 */
public record ModuleBackendCodeDetail(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, String backendLogic, String apiSummary, String reviewComment,
		List<GeneratedCodeFile> files) {
}
