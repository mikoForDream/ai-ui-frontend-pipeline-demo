package com.pig4cloud.pig.workflow.service;

import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiDesignRendererTest {

	private final UiDesignRenderer renderer = new UiDesignRenderer();

	@Test
	void rendersCompleteInteractiveUiAndEscapesUserText() {
		WorkflowProject project = new WorkflowProject();
		project.setName("客户<script>alert(1)</script>平台");
		WorkflowModule module = new WorkflowModule();
		module.setModuleCode("USER_ADMIN");
		module.setName("用户<img src=x onerror=alert(1)>管理");
		WorkflowFeature feature = new WorkflowFeature();
		feature.setFeatureCode("FEAT_001");
		feature.setName("创建用户");
		feature.setAcceptanceCriteria("保存后显示 <b>成功</b>");
		feature.setPriority("HIGH");

		String html = renderer.render(project, module, List.of(feature));

		assertTrue(html.startsWith("<!doctype html>"));
		assertTrue(html.contains("<style>"));
		assertTrue(html.contains("<script>document.querySelectorAll('.nav-link')"));
		assertTrue(html.contains("data-row=\"0\""));
		assertTrue(html.contains("客户&lt;script&gt;alert(1)&lt;/script&gt;平台"));
		assertTrue(html.contains("用户&lt;img src=x onerror=alert(1)&gt;管理"));
		assertTrue(html.contains("保存后显示 &lt;b&gt;成功&lt;/b&gt;"));
		assertFalse(html.contains("<script>alert(1)</script>"));
		assertFalse(html.contains("<img src=x onerror=alert(1)>"));
	}

}
