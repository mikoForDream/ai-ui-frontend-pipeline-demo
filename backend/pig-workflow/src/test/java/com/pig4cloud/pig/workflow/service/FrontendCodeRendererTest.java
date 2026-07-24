package com.pig4cloud.pig.workflow.service;

import com.pig4cloud.pig.workflow.dto.GeneratedCodeFile;
import com.pig4cloud.pig.workflow.entity.WorkflowFeature;
import com.pig4cloud.pig.workflow.entity.WorkflowModule;
import com.pig4cloud.pig.workflow.entity.WorkflowProject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendCodeRendererTest {

	private final FrontendCodeRenderer renderer = new FrontendCodeRenderer();

	@Test
	void rendersReviewableVuePackageAndEscapesUserText() {
		WorkflowProject project = new WorkflowProject();
		project.setName("客户<script>alert(1)</script>平台");
		WorkflowModule module = new WorkflowModule();
		module.setId(12L);
		module.setModuleCode("USER Admin/../");
		module.setName("用户<img src=x onerror=alert(1)>管理");
		WorkflowFeature feature = new WorkflowFeature();
		feature.setFeatureCode("FEAT_'001");
		feature.setName("创建\\用户\n<script>alert(2)</script>");
		feature.setAcceptanceCriteria("保存后显示 <b>成功</b>");

		FrontendCodeRenderer.RenderedFrontend result = renderer.render(project, module, List.of(feature),
				"点击保存后提示 <script>alert(3)</script>");
		Map<String, GeneratedCodeFile> files = result.files().stream()
			.collect(Collectors.toMap(GeneratedCodeFile::path, Function.identity()));

		assertEquals(4, files.size());
		assertTrue(files.containsKey("src/views/workflow/user-admin/index.vue"));
		assertTrue(files.containsKey("src/api/workflow/user-admin.ts"));
		assertTrue(files.containsKey("src/types/workflow/user-admin.ts"));
		assertTrue(files.containsKey("docs/frontend/user-admin.md"));
		assertTrue(files.get("src/views/workflow/user-admin/index.vue").content().contains("priority: 'MEDIUM'"));
		assertTrue(files.get("src/views/workflow/user-admin/index.vue").content().contains("\\x3Cscript\\x3Ealert(2)\\x3C/script\\x3E"));
		assertFalse(files.get("src/views/workflow/user-admin/index.vue").content().contains("<script>alert(2)</script>"));
		assertTrue(files.get("src/types/workflow/user-admin.ts").content().contains("FEAT_\\'001"));
		assertTrue(result.previewHtml().contains("document.querySelectorAll('button')"));
		assertTrue(result.previewHtml().contains("客户&lt;script&gt;alert(1)&lt;/script&gt;平台"));
		assertTrue(result.previewHtml().contains("用户&lt;img src=x onerror=alert(1)&gt;管理"));
		assertFalse(result.previewHtml().contains("<script>alert(3)</script>"));
	}

	@Test
	void usesStableFallbackForModuleCodeWithoutAsciiCharacters() {
		WorkflowProject project = new WorkflowProject();
		project.setName("项目");
		WorkflowModule module = new WorkflowModule();
		module.setId(8L);
		module.setModuleCode("用户管理");
		module.setName("用户管理");

		FrontendCodeRenderer.RenderedFrontend result = renderer.render(project, module, List.of(feature()), null);

		assertTrue(result.files().stream().allMatch(file -> file.path().contains("module-8")));
	}

	private WorkflowFeature feature() {
		WorkflowFeature feature = new WorkflowFeature();
		feature.setFeatureCode("FEAT_001");
		feature.setName("查询");
		feature.setPriority("HIGH");
		return feature;
	}
}
