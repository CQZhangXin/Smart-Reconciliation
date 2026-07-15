-- 自定义对账模块增量脚本（已有库可单独执行）
USE recon_platform;

CREATE TABLE IF NOT EXISTS custom_recon_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    ledger_id       BIGINT        COMMENT '账套ID',
    def_name        VARCHAR(200)  NOT NULL COMMENT '方案名称',
    def_code        VARCHAR(50)   NOT NULL COMMENT '方案编码',
    description     TEXT          COMMENT '方案描述',
    source_a_id     BIGINT        NOT NULL COMMENT '主数据源A',
    source_b_ids    JSON          NOT NULL COMMENT '对账方数据源ID列表(支持多源)',
    rule_ids        JSON          COMMENT '选用规则ID列表',
    match_layers    JSON          COMMENT '启用匹配层: {exact,rule,ai,split}',
    period_type     VARCHAR(20)   DEFAULT 'MONTHLY' COMMENT '期间类型: DAILY/MONTHLY/CUSTOM',
    default_period  VARCHAR(20)   COMMENT '默认对账期间',
    status          VARCHAR(20)   DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/INACTIVE',
    last_run_task_id BIGINT       COMMENT '最近一次运行任务ID',
    last_run_at     DATETIME      COMMENT '最近运行时间',
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_def_code (org_id, def_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义对账方案定义表';

INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path) VALUES
(9, 0, '自定义对账', 'custom-recon:view', 'MENU', '/custom-recon'),
(10, 9, '执行自定义对账', 'custom-recon:run', 'BUTTON', NULL),
(11, 7, '大模型配置', 'system:ai', 'MENU', '/system/ai');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, 9 FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 9
);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, 10 FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 10
);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, 11 FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 11
);
