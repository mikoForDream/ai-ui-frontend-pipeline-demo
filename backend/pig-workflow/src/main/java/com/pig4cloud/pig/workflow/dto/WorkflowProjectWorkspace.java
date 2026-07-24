package com.pig4cloud.pig.workflow.dto;

import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;

import java.util.List;

/** 研发项目工作台聚合数据。 */
public record WorkflowProjectWorkspace(WorkflowProject project, List<WorkflowMaterialSummary> materials,
		List<WorkflowModule> modules, List<WorkflowFeature> features, List<ModulePrototypeSummary> prototypes,
		List<ModuleUiDesignSummary> uiDesigns, String frozenSpecVersion) {
}
