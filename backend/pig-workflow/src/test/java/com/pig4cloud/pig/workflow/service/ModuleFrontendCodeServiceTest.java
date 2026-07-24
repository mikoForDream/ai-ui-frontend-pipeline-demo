package com.pig4cloud.pig.workflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleFrontendCodeServiceTest {

	@Test
	void acceptsOnlyRelativeArchivePathsWithoutTraversal() {
		assertTrue(ModuleFrontendCodeService.isSafeArchivePath("src/views/users/index.vue"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("../secret.txt"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("src/../secret.txt"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("/etc/passwd"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("C:\\temp\\secret.txt"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("C:secret.txt"));
		assertFalse(ModuleFrontendCodeService.isSafeArchivePath("src//index.vue"));
	}
}
