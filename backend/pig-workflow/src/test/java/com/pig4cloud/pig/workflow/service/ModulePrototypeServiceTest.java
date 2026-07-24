package com.pig4cloud.pig.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulePrototypeServiceTest {

	private final ModulePrototypeService service = new ModulePrototypeService(new ObjectMapper(), null, null, null,
			null, null, null);

	@Test
	void rendersInteractivePrototypeAndEscapesRequirementText() {
		WorkflowProject project = new WorkflowProject();
		project.setName("客户管理平台");
		WorkflowModule module = new WorkflowModule();
		module.setName("用户<script>alert(1)</script>管理");
		WorkflowFeature feature = new WorkflowFeature();
		feature.setFeatureCode("FEAT_001");
		feature.setName("创建用户");
		feature.setDescription("录入 <img src=x onerror=alert(1)> 资料");
		feature.setAcceptanceCriteria("保存后展示成功反馈");
		feature.setPriority("HIGH");

		String html = service.renderHtml(project, module, List.of(feature));

		assertTrue(html.contains("<!doctype html>"));
		assertTrue(html.contains("data-target=\"feature-0\""));
		assertTrue(html.contains("操作已完成，原型交互反馈正常"));
		assertTrue(html.contains("用户&lt;script&gt;alert(1)&lt;/script&gt;管理"));
		assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
		assertFalse(html.contains("<script>alert(1)</script>"));
	}

}
