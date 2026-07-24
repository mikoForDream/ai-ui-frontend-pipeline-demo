-- AI 工作流核心表与首批权限资源（MySQL 8+）
-- mysql CLI may default to latin1 in minimal containers; switch before reading any UTF-8 literals.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wf_definition` (
  `id` bigint NOT NULL,
  `code` varchar(64) NOT NULL COMMENT '稳定业务编码',
  `name` varchar(128) NOT NULL,
  `version` int NOT NULL DEFAULT 1,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `description` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_definition_code_version` (`code`, `version`, `del_flag`)
) ENGINE=InnoDB COMMENT='工作流定义';

CREATE TABLE IF NOT EXISTS `wf_node_definition` (
  `id` bigint NOT NULL,
  `definition_id` bigint NOT NULL,
  `node_key` varchar(64) NOT NULL,
  `node_name` varchar(128) NOT NULL,
  `node_type` varchar(32) NOT NULL COMMENT 'AI、MANUAL、SERVICE 等',
  `sort_order` int NOT NULL DEFAULT 0,
  `start_node` tinyint(1) NOT NULL DEFAULT 0,
  `end_node` tinyint(1) NOT NULL DEFAULT 0,
  `next_node_key` varchar(64) DEFAULT NULL,
  `config_json` longtext DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_node_key` (`definition_id`, `node_key`, `del_flag`),
  KEY `idx_wf_node_definition` (`definition_id`, `sort_order`)
) ENGINE=InnoDB COMMENT='工作流节点定义';

CREATE TABLE IF NOT EXISTS `wf_instance` (
  `id` bigint NOT NULL,
  `definition_id` bigint NOT NULL,
  `business_key` varchar(128) NOT NULL,
  `title` varchar(200) NOT NULL,
  `status` varchar(20) NOT NULL,
  `current_node_key` varchar(64) DEFAULT NULL,
  `input_json` longtext DEFAULT NULL,
  `output_json` longtext DEFAULT NULL,
  `started_by` varchar(64) NOT NULL,
  `started_at` datetime NOT NULL,
  `finished_at` datetime DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_instance_business` (`definition_id`, `business_key`, `del_flag`),
  KEY `idx_wf_instance_status` (`status`, `create_time`)
) ENGINE=InnoDB COMMENT='工作流实例';

CREATE TABLE IF NOT EXISTS `wf_task` (
  `id` bigint NOT NULL,
  `instance_id` bigint NOT NULL,
  `node_definition_id` bigint NOT NULL,
  `node_key` varchar(64) NOT NULL,
  `node_name` varchar(128) NOT NULL,
  `task_type` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `input_json` longtext DEFAULT NULL,
  `output_json` longtext DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_wf_task_instance` (`instance_id`, `create_time`),
  KEY `idx_wf_task_pending` (`status`, `assignee_id`, `create_time`)
) ENGINE=InnoDB COMMENT='工作流节点任务';

CREATE TABLE IF NOT EXISTS `wf_execution_log` (
  `id` bigint NOT NULL,
  `instance_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `node_key` varchar(64) DEFAULT NULL,
  `event_type` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL,
  `request_json` longtext DEFAULT NULL,
  `response_json` longtext DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wf_log_instance` (`instance_id`, `create_time`),
  KEY `idx_wf_log_task` (`task_id`)
) ENGINE=InnoDB COMMENT='工作流执行日志';

-- 工作流菜单与权限资源。
INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `en_name`, `path`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`)
VALUES (10000, '工作流管理', 'workflow', '/workflow', -1, '1', 5, '0', 'admin');

UPDATE `sys_menu`
SET `name` = '工作流管理', `en_name` = 'workflow', `path` = '/workflow', `icon` = 'ele-Connection', `visible` = '1', `menu_type` = '0'
WHERE `menu_id` = 10000;

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `en_name`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10020, '流程定义', 'definitions', '/workflow/definition/index', 10000, 'ele-SetUp', '1', 1, '0', 'admin'),
(10030, '流程实例', 'instances', '/workflow/instance/index', 10000, 'ele-VideoPlay', '1', 2, '0', 'admin'),
(10040, '人工审核', 'approvals', '/workflow/approval/index', 10000, 'ele-Checked', '1', 3, '0', 'admin');

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10001, '流程定义查询', 'workflow_definition_view', 10000, '0', 1, '1', 'admin'),
(10002, '流程定义新增', 'workflow_definition_add', 10000, '0', 2, '1', 'admin'),
(10003, '流程定义编辑', 'workflow_definition_edit', 10000, '0', 3, '1', 'admin'),
(10004, '流程定义发布', 'workflow_definition_publish', 10000, '0', 4, '1', 'admin'),
(10005, '流程实例查询', 'workflow_instance_view', 10000, '0', 5, '1', 'admin'),
(10006, '流程实例启动', 'workflow_instance_start', 10000, '0', 6, '1', 'admin'),
(10007, '流程任务查询', 'workflow_task_view', 10000, '0', 7, '1', 'admin'),
(10008, '流程任务执行', 'workflow_task_execute', 10000, '0', 8, '1', 'admin'),
(10009, '流程任务重试', 'workflow_task_retry', 10000, '0', 9, '1', 'admin');

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10010, '审核记录查询', 'workflow_approval_view', 10000, '0', 10, '1', 'admin'),
(10011, '审核待办领取', 'workflow_approval_claim', 10000, '0', 11, '1', 'admin'),
(10012, '审核决定提交', 'workflow_approval_decide', 10000, '0', 12, '1', 'admin');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10000), (1, 10001), (1, 10002), (1, 10003), (1, 10004),
(1, 10005), (1, 10006), (1, 10007), (1, 10008), (1, 10009);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10010), (1, 10011), (1, 10012);

UPDATE `sys_menu` SET `parent_id` = 10020 WHERE `menu_id` IN (10001, 10002, 10003, 10004);
UPDATE `sys_menu` SET `parent_id` = 10030 WHERE `menu_id` IN (10005, 10006, 10007, 10008, 10009);
UPDATE `sys_menu` SET `parent_id` = 10040 WHERE `menu_id` IN (10010, 10011, 10012);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10020), (1, 10030), (1, 10040);

-- V2：项目、动作流转、人工审核、产物版本和统一产品规格。
SET @wf_definition_project_id_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_definition' AND column_name = 'project_id'
);
SET @wf_definition_project_id_sql = IF(
  @wf_definition_project_id_exists = 0,
  'ALTER TABLE `wf_definition` ADD COLUMN `project_id` bigint DEFAULT NULL AFTER `id`',
  'SELECT 1'
);
PREPARE wf_definition_project_id_stmt FROM @wf_definition_project_id_sql;
EXECUTE wf_definition_project_id_stmt;
DEALLOCATE PREPARE wf_definition_project_id_stmt;

SET @wf_instance_project_id_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_instance' AND column_name = 'project_id'
);
SET @wf_instance_project_id_sql = IF(
  @wf_instance_project_id_exists = 0,
  'ALTER TABLE `wf_instance` ADD COLUMN `project_id` bigint DEFAULT NULL AFTER `id`',
  'SELECT 1'
);
PREPARE wf_instance_project_id_stmt FROM @wf_instance_project_id_sql;
EXECUTE wf_instance_project_id_stmt;
DEALLOCATE PREPARE wf_instance_project_id_stmt;

CREATE TABLE IF NOT EXISTS `wf_project` (
  `id` bigint NOT NULL,
  `project_code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `repository_url` varchar(500) DEFAULT NULL,
  `default_branch` varchar(128) DEFAULT NULL,
  `frontend_path` varchar(255) DEFAULT NULL,
  `backend_path` varchar(255) DEFAULT NULL,
  `tech_stack` varchar(1000) DEFAULT NULL,
  `current_spec_version` varchar(64) DEFAULT NULL,
  `notion_page_id` varchar(64) DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_project_code` (`project_code`, `del_flag`),
  KEY `idx_wf_project_status` (`status`, `create_time`)
) ENGINE=InnoDB COMMENT='工作流研发项目';

CREATE TABLE IF NOT EXISTS `wf_transition` (
  `id` bigint NOT NULL,
  `definition_id` bigint NOT NULL,
  `source_node_key` varchar(64) NOT NULL,
  `target_node_key` varchar(64) DEFAULT NULL,
  `action` varchar(32) NOT NULL,
  `condition_expression` varchar(1000) DEFAULT NULL,
  `priority` int NOT NULL DEFAULT 0,
  `default_transition` tinyint(1) NOT NULL DEFAULT 0,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_transition_route` (`definition_id`, `source_node_key`, `action`, `priority`, `del_flag`),
  KEY `idx_wf_transition_lookup` (`definition_id`, `source_node_key`, `action`, `priority`)
) ENGINE=InnoDB COMMENT='工作流节点动作流转规则';

CREATE TABLE IF NOT EXISTS `wf_approval` (
  `id` bigint NOT NULL,
  `instance_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `approval_type` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `decision` varchar(20) DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `reviewer_name` varchar(64) DEFAULT NULL,
  `candidate_reviewer_id` bigint DEFAULT NULL,
  `candidate_role_id` bigint DEFAULT NULL,
  `comment` text DEFAULT NULL,
  `artifact_version_id` bigint DEFAULT NULL,
  `operation_key` varchar(128) NOT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_approval_operation` (`operation_key`),
  KEY `idx_wf_approval_pending` (`status`, `reviewer_id`, `create_time`),
  KEY `idx_wf_approval_candidate` (`status`, `candidate_reviewer_id`, `candidate_role_id`, `create_time`),
  KEY `idx_wf_approval_instance` (`instance_id`, `create_time`)
) ENGINE=InnoDB COMMENT='工作流人工审核记录';

SET @wf_approval_candidate_reviewer_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_approval' AND column_name = 'candidate_reviewer_id'
);
SET @wf_approval_candidate_reviewer_sql = IF(
  @wf_approval_candidate_reviewer_exists = 0,
  'ALTER TABLE `wf_approval` ADD COLUMN `candidate_reviewer_id` bigint DEFAULT NULL AFTER `reviewer_name`',
  'SELECT 1'
);
PREPARE wf_approval_candidate_reviewer_stmt FROM @wf_approval_candidate_reviewer_sql;
EXECUTE wf_approval_candidate_reviewer_stmt;
DEALLOCATE PREPARE wf_approval_candidate_reviewer_stmt;

SET @wf_approval_candidate_role_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_approval' AND column_name = 'candidate_role_id'
);
SET @wf_approval_candidate_role_sql = IF(
  @wf_approval_candidate_role_exists = 0,
  'ALTER TABLE `wf_approval` ADD COLUMN `candidate_role_id` bigint DEFAULT NULL AFTER `candidate_reviewer_id`',
  'SELECT 1'
);
PREPARE wf_approval_candidate_role_stmt FROM @wf_approval_candidate_role_sql;
EXECUTE wf_approval_candidate_role_stmt;
DEALLOCATE PREPARE wf_approval_candidate_role_stmt;

SET @wf_approval_candidate_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'wf_approval' AND index_name = 'idx_wf_approval_candidate'
);
SET @wf_approval_candidate_index_sql = IF(
  @wf_approval_candidate_index_exists = 0,
  'ALTER TABLE `wf_approval` ADD INDEX `idx_wf_approval_candidate` (`status`, `candidate_reviewer_id`, `candidate_role_id`, `create_time`)',
  'SELECT 1'
);
PREPARE wf_approval_candidate_index_stmt FROM @wf_approval_candidate_index_sql;
EXECUTE wf_approval_candidate_index_stmt;
DEALLOCATE PREPARE wf_approval_candidate_index_stmt;

CREATE TABLE IF NOT EXISTS `wf_artifact` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `instance_id` bigint DEFAULT NULL,
  `artifact_code` varchar(128) NOT NULL,
  `name` varchar(200) NOT NULL,
  `artifact_type` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `current_version_id` bigint DEFAULT NULL,
  `notion_page_id` varchar(64) DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_artifact_code` (`project_id`, `artifact_code`, `del_flag`),
  KEY `idx_wf_artifact_instance` (`instance_id`, `artifact_type`)
) ENGINE=InnoDB COMMENT='工作流产物';

CREATE TABLE IF NOT EXISTS `wf_artifact_version` (
  `id` bigint NOT NULL,
  `artifact_id` bigint NOT NULL,
  `version_no` varchar(64) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_url` varchar(1000) DEFAULT NULL,
  `file_id` bigint DEFAULT NULL,
  `repository_url` varchar(500) DEFAULT NULL,
  `branch_name` varchar(128) DEFAULT NULL,
  `commit_sha` varchar(64) DEFAULT NULL,
  `content_json` longtext DEFAULT NULL,
  `checksum` varchar(128) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_artifact_version` (`artifact_id`, `version_no`),
  KEY `idx_wf_artifact_version_status` (`artifact_id`, `status`, `create_time`)
) ENGINE=InnoDB COMMENT='工作流产物不可覆盖版本';

CREATE TABLE IF NOT EXISTS `wf_product_spec` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `instance_id` bigint DEFAULT NULL,
  `artifact_version_id` bigint DEFAULT NULL,
  `schema_version` varchar(32) NOT NULL,
  `version_no` varchar(64) NOT NULL,
  `spec_json` longtext NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `frozen_at` datetime DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_product_spec_version` (`project_id`, `version_no`, `del_flag`),
  KEY `idx_wf_product_spec_status` (`project_id`, `status`, `create_time`)
) ENGINE=InnoDB COMMENT='前后端统一产品规格';

-- V3：研发项目资料、模块与功能点审核。
SET @wf_project_current_stage_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_project' AND column_name = 'current_stage'
);
SET @wf_project_current_stage_sql = IF(
  @wf_project_current_stage_exists = 0,
  'ALTER TABLE `wf_project` ADD COLUMN `current_stage` varchar(32) NOT NULL DEFAULT ''MATERIAL_COLLECTION'' AFTER `status`',
  'SELECT 1'
);
PREPARE wf_project_current_stage_stmt FROM @wf_project_current_stage_sql;
EXECUTE wf_project_current_stage_stmt;
DEALLOCATE PREPARE wf_project_current_stage_stmt;

CREATE TABLE IF NOT EXISTS `wf_material` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `object_name` varchar(500) NOT NULL,
  `bucket_name` varchar(128) NOT NULL,
  `content_type` varchar(128) DEFAULT NULL,
  `extension` varchar(20) NOT NULL,
  `file_size` bigint NOT NULL,
  `checksum` varchar(128) NOT NULL,
  `parse_status` varchar(32) NOT NULL DEFAULT 'UPLOADED',
  `extracted_text` longtext DEFAULT NULL,
  `parse_message` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_wf_material_project` (`project_id`, `parse_status`, `create_time`),
  KEY `idx_wf_material_checksum` (`project_id`, `checksum`, `del_flag`)
) ENGINE=InnoDB COMMENT='研发项目上传资料';

CREATE TABLE IF NOT EXISTS `wf_module` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `module_code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'REQUIREMENT_REVIEW',
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_module_code` (`project_id`, `module_code`, `del_flag`),
  KEY `idx_wf_module_project` (`project_id`, `sort_order`)
) ENGINE=InnoDB COMMENT='研发项目功能模块';

CREATE TABLE IF NOT EXISTS `wf_feature` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `module_id` bigint NOT NULL,
  `feature_code` varchar(64) NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `acceptance_criteria` text DEFAULT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'MEDIUM',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  `review_comment` text DEFAULT NULL,
  `version` int NOT NULL DEFAULT 1,
  `create_by` varchar(64) NOT NULL DEFAULT ' ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) NOT NULL DEFAULT ' ',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_feature_code` (`project_id`, `feature_code`, `del_flag`),
  KEY `idx_wf_feature_review` (`project_id`, `module_id`, `status`, `create_time`)
) ENGINE=InnoDB COMMENT='研发项目功能点';

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `en_name`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10015, '研发项目', 'projects', '/workflow/project/index', 10000, 'ele-Briefcase', '1', 1, '0', 'admin');

UPDATE `sys_menu` SET `sort_order` = 2 WHERE `menu_id` = 10020;
UPDATE `sys_menu` SET `sort_order` = 3 WHERE `menu_id` = 10030;
UPDATE `sys_menu` SET `sort_order` = 4 WHERE `menu_id` = 10040;

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10013, '研发项目查询', 'workflow_project_view', 10015, '0', 1, '1', 'admin'),
(10014, '研发项目编辑', 'workflow_project_edit', 10015, '0', 2, '1', 'admin'),
(10016, '项目资料上传', 'workflow_material_upload', 10015, '0', 3, '1', 'admin'),
(10017, '项目资料分析', 'workflow_material_analyze', 10015, '0', 4, '1', 'admin'),
(10018, '功能点编辑', 'workflow_feature_edit', 10015, '0', 5, '1', 'admin'),
(10019, '功能点审核', 'workflow_feature_review', 10015, '0', 6, '1', 'admin');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10013), (1, 10014), (1, 10015), (1, 10016), (1, 10017), (1, 10018), (1, 10019);

-- V4: module-scoped, versioned interactive prototype review.
SET @wf_artifact_module_id_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_artifact' AND column_name = 'module_id'
);
SET @wf_artifact_module_id_sql = IF(
  @wf_artifact_module_id_exists = 0,
  'ALTER TABLE `wf_artifact` ADD COLUMN `module_id` bigint DEFAULT NULL AFTER `instance_id`, ADD INDEX `idx_wf_artifact_module` (`project_id`, `module_id`, `artifact_type`)',
  'SELECT 1'
);
PREPARE wf_artifact_module_id_stmt FROM @wf_artifact_module_id_sql;
EXECUTE wf_artifact_module_id_stmt;
DEALLOCATE PREPARE wf_artifact_module_id_stmt;

SET @wf_artifact_version_review_comment_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_artifact_version' AND column_name = 'review_comment'
);
SET @wf_artifact_version_review_comment_sql = IF(
  @wf_artifact_version_review_comment_exists = 0,
  'ALTER TABLE `wf_artifact_version` ADD COLUMN `review_comment` varchar(1000) DEFAULT NULL AFTER `status`',
  'SELECT 1'
);
PREPARE wf_artifact_version_review_comment_stmt FROM @wf_artifact_version_review_comment_sql;
EXECUTE wf_artifact_version_review_comment_stmt;
DEALLOCATE PREPARE wf_artifact_version_review_comment_stmt;

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10021, '模块原型生成', 'workflow_prototype_generate', 10015, '0', 7, '1', 'admin'),
(10022, '模块原型审核', 'workflow_prototype_review', 10015, '0', 8, '1', 'admin');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10021), (1, 10022);

-- V5: module UI draft generation, user design upload and review.
INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10023, '模块 UI 草稿生成', 'workflow_ui_generate', 10015, '0', 9, '1', 'admin'),
(10024, '模块设计图上传', 'workflow_ui_upload', 10015, '0', 10, '1', 'admin'),
(10025, '模块 UI 审核', 'workflow_ui_review', 10015, '0', 11, '1', 'admin');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10023), (1, 10024), (1, 10025);

-- V6: module frontend implementation notes, code generation and review.
SET @wf_module_frontend_logic_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_module' AND column_name = 'frontend_logic'
);
SET @wf_module_frontend_logic_sql = IF(
  @wf_module_frontend_logic_exists = 0,
  'ALTER TABLE `wf_module` ADD COLUMN `frontend_logic` text DEFAULT NULL AFTER `description`',
  'SELECT 1'
);
PREPARE wf_module_frontend_logic_stmt FROM @wf_module_frontend_logic_sql;
EXECUTE wf_module_frontend_logic_stmt;
DEALLOCATE PREPARE wf_module_frontend_logic_stmt;

INSERT IGNORE INTO `sys_menu` (`menu_id`, `name`, `permission`, `parent_id`, `visible`, `sort_order`, `menu_type`, `create_by`) VALUES
(10026, '模块前端逻辑编辑', 'workflow_frontend_edit', 10015, '0', 12, '1', 'admin'),
(10027, '模块前端代码生成', 'workflow_frontend_generate', 10015, '0', 13, '1', 'admin'),
(10028, '模块前端代码审核', 'workflow_frontend_review', 10015, '0', 14, '1', 'admin');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10026), (1, 10027), (1, 10028);

-- INSERT IGNORE preserves existing rows, so repair menu labels after any earlier import with a wrong client charset.
UPDATE `sys_menu`
SET `name` = CASE `menu_id`
  WHEN 10000 THEN '工作流管理'
  WHEN 10001 THEN '流程定义查询'
  WHEN 10002 THEN '流程定义新增'
  WHEN 10003 THEN '流程定义编辑'
  WHEN 10004 THEN '流程定义发布'
  WHEN 10005 THEN '流程实例查询'
  WHEN 10006 THEN '流程实例启动'
  WHEN 10007 THEN '流程任务查询'
  WHEN 10008 THEN '流程任务执行'
  WHEN 10009 THEN '流程任务重试'
  WHEN 10010 THEN '审核记录查询'
  WHEN 10011 THEN '审核待办领取'
  WHEN 10012 THEN '审核决定提交'
  WHEN 10013 THEN '研发项目查询'
  WHEN 10014 THEN '研发项目编辑'
  WHEN 10015 THEN '研发项目'
  WHEN 10016 THEN '项目资料上传'
  WHEN 10017 THEN '项目资料分析'
  WHEN 10018 THEN '功能点编辑'
  WHEN 10019 THEN '功能点审核'
  WHEN 10020 THEN '流程定义'
  WHEN 10021 THEN '模块原型生成'
  WHEN 10022 THEN '模块原型审核'
  WHEN 10023 THEN '模块 UI 草稿生成'
  WHEN 10024 THEN '模块设计图上传'
  WHEN 10025 THEN '模块 UI 审核'
  WHEN 10026 THEN '模块前端逻辑编辑'
  WHEN 10027 THEN '模块前端代码生成'
  WHEN 10028 THEN '模块前端代码审核'
  WHEN 10030 THEN '流程实例'
  WHEN 10040 THEN '人工审核'
  ELSE `name`
END
WHERE `menu_id` IN (
  10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009,
  10010, 10011, 10012, 10013, 10014, 10015, 10016, 10017, 10018, 10019,
  10020, 10021, 10022, 10023, 10024, 10025, 10026, 10027, 10028, 10030, 10040
);
