-- AI 工作流核心表与首批权限资源（MySQL 8+）

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
