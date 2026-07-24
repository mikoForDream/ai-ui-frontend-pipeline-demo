package com.pig4cloud.pig.workflow.dto;

import java.util.List;

/** 可审查和预览的模块前端代码版本。 */
public record ModuleFrontendCodeDetail(Long artifactId, Long moduleId, Long versionId, String versionNo,
		String status, String generator, Long uiDesignVersionId, String frontendLogic, String reviewComment,
		String previewHtml, List<GeneratedCodeFile> files) {
}
