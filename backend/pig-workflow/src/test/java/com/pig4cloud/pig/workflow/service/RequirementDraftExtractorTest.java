package com.pig4cloud.pig.workflow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequirementDraftExtractorTest {

	private final RequirementDraftExtractor extractor = new RequirementDraftExtractor();

	@Test
	void extractsFeaturesUnderExplicitModules() {
		List<RequirementDraftExtractor.DraftModule> result = extractor.extract(List.of("""
				模块：用户管理
				- 用户可以创建账号
				- 管理员可以停用账号
				模块：订单管理
				1. 用户提交订单
				2. 用户查看订单状态
				"""));

		assertEquals(2, result.size());
		assertEquals("用户管理", result.get(0).name());
		assertEquals(List.of("用户可以创建账号", "管理员可以停用账号"), result.get(0).features());
		assertEquals("订单管理", result.get(1).name());
	}

	@Test
	void usesCoreModuleWhenNoModuleHeadingExists() {
		List<RequirementDraftExtractor.DraftModule> result = extractor.extract(List.of("""
				- 上传需求文档
				- 审核功能点
				"""));

		assertEquals(1, result.size());
		assertEquals("核心业务", result.get(0).name());
		assertEquals(2, result.get(0).features().size());
	}

	@Test
	void rejectsDocumentsWithoutListFeatures() {
		assertThrows(IllegalArgumentException.class, () -> extractor.extract(List.of("这是一段没有结构的描述。")));
	}

}
