package com.pig4cloud.pig.workflow.dto;

/** 需求分析产生的模块和功能点数量。 */
public record RequirementAnalysisResult(int moduleCount, int featureCount, String analyzer) {
}
