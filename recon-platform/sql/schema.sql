-- ============================================================================
-- 智能 AI 对账平台 - MySQL 数据库初始化脚本
-- Version: 1.0.0
-- Database: MySQL 8.0+
-- ============================================================================

CREATE DATABASE IF NOT EXISTS recon_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE recon_platform;

-- ============================================================================
-- 1. 组织架构模块
-- ============================================================================

-- 组织/公司
CREATE TABLE org_organization (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT        DEFAULT 0 COMMENT '上级组织ID',
    org_name        VARCHAR(200)  NOT NULL COMMENT '组织名称',
    org_code        VARCHAR(50)   NOT NULL COMMENT '组织编码',
    org_type        VARCHAR(20)   DEFAULT 'COMPANY' COMMENT '类型: GROUP/COMPANY/DEPARTMENT',
    contact_name    VARCHAR(100)  COMMENT '联系人',
    contact_phone   VARCHAR(30)   COMMENT '联系电话',
    address         VARCHAR(500)  COMMENT '地址',
    status          VARCHAR(20)   DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    sort_order      INT           DEFAULT 0 COMMENT '排序',
    extra_info      JSON          COMMENT '扩展信息',
    created_by      BIGINT        COMMENT '创建人',
    updated_by      BIGINT        COMMENT '更新人',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_org_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织表';

-- 账套
CREATE TABLE org_ledger (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    ledger_name     VARCHAR(200)  NOT NULL COMMENT '账套名称',
    ledger_code     VARCHAR(50)   NOT NULL COMMENT '账套编码',
    currency        VARCHAR(3)    DEFAULT 'CNY' COMMENT '本位币',
    accounting_std  VARCHAR(20)   DEFAULT 'CAS' COMMENT '会计准则: CAS/IFRS/GAAP',
    fiscal_year_start DATE        COMMENT '会计年度起始月',
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    UNIQUE KEY uk_ledger_code (ledger_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账套表';

-- 账户/科目
CREATE TABLE org_account (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    ledger_id       BIGINT        NOT NULL COMMENT '账套ID',
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    account_code    VARCHAR(50)   NOT NULL COMMENT '科目编码',
    account_name    VARCHAR(200)  NOT NULL COMMENT '科目名称',
    account_type    VARCHAR(30)   COMMENT '类型: BANK/CASH/AR/AP/REVENUE/EXPENSE',
    bank_name       VARCHAR(200)  COMMENT '银行名称',
    bank_account_no VARCHAR(100)  COMMENT '银行账号(加密存储)',
    currency        VARCHAR(3)    DEFAULT 'CNY',
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_ledger_id (ledger_id),
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- ============================================================================
-- 2. 数据源管理模块
-- ============================================================================

-- 数据源配置
CREATE TABLE datasource (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    ledger_id       BIGINT        COMMENT '账套ID',
    ds_name         VARCHAR(200)  NOT NULL COMMENT '数据源名称',
    ds_type         VARCHAR(30)   NOT NULL COMMENT '类型: BANK_API/THIRD_PAYMENT/ERP/FILE_IMPORT/MANUAL',
    ds_category     VARCHAR(20)   DEFAULT 'SOURCE_A' COMMENT '来源方: SOURCE_A/SOURCE_B',
    provider        VARCHAR(100)  COMMENT '提供商(工行/支付宝/SAP等)',
    conn_config     JSON          COMMENT '连接配置(加密存储)',
    field_mapping   JSON          COMMENT '字段映射配置',
    sync_strategy   VARCHAR(20)   DEFAULT 'MANUAL' COMMENT '同步策略: MANUAL/SCHEDULED/EVENT_DRIVEN/REALTIME',
    sync_cron       VARCHAR(50)   COMMENT '定时Cron表达式',
    last_sync_at    DATETIME      COMMENT '最后同步时间',
    last_sync_status VARCHAR(20)  COMMENT '最后同步状态: SUCCESS/FAILED/PARTIAL',
    health_status   VARCHAR(20)   DEFAULT 'UNKNOWN' COMMENT '健康状态: HEALTHY/UNHEALTHY/UNKNOWN',
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    description     TEXT          COMMENT '描述',
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    INDEX idx_ds_type (ds_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';

-- 原始交易记录
CREATE TABLE raw_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id       BIGINT        NOT NULL COMMENT '数据源ID',
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    batch_id        VARCHAR(100)  COMMENT '批次号',
    trace_id        VARCHAR(100)  COMMENT '追踪ID(幂等键)',
    raw_data        JSON          COMMENT '原始数据',
    normalized_data JSON          COMMENT '标准化后数据',
    amount          DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    currency        VARCHAR(3)    DEFAULT 'CNY' COMMENT '币种',
    transaction_date DATE         COMMENT '交易日期',
    booking_date    DATE          COMMENT '记账日期',
    value_date      DATE          COMMENT '起息日',
    transaction_ref VARCHAR(500)  COMMENT '交易参考号/流水号',
    description     TEXT          COMMENT '摘要/描述',
    counter_party   VARCHAR(300)  COMMENT '对方名称',
    counter_acct    VARCHAR(100)  COMMENT '对方账号',
    direction       VARCHAR(10)   COMMENT '借贷方向: DEBIT/CREDIT',
    balance         DECIMAL(18,2) COMMENT '余额',
    fee_amount      DECIMAL(18,2) COMMENT '手续费',
    extra_info      JSON          COMMENT '扩展字段',
    hash_value      VARCHAR(64)   COMMENT '数据指纹(SHA-256去重)',
    status          VARCHAR(20)   DEFAULT 'PENDING' COMMENT 'PENDING/NORMALIZED/MATCHED/UNMATCHED/IGNORED',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source_id (source_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_org_id (org_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_amount (amount),
    UNIQUE KEY uk_trace_id (source_id, trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始交易记录表';

-- 数据同步日志
CREATE TABLE datasource_sync_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id       BIGINT        NOT NULL COMMENT '数据源ID',
    org_id          BIGINT        NOT NULL,
    sync_type       VARCHAR(20)   COMMENT 'FULL/INCREMENTAL',
    total_count     INT           DEFAULT 0 COMMENT '总条数',
    success_count   INT           DEFAULT 0 COMMENT '成功条数',
    error_count     INT           DEFAULT 0 COMMENT '失败条数',
    sync_status     VARCHAR(20)   COMMENT 'RUNNING/SUCCESS/FAILED/PARTIAL',
    error_msg       TEXT          COMMENT '错误信息',
    started_at      DATETIME      COMMENT '开始时间',
    completed_at    DATETIME      COMMENT '完成时间',
    duration_ms     BIGINT        COMMENT '耗时(毫秒)',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source_id (source_id),
    INDEX idx_sync_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据同步日志表';

-- ============================================================================
-- 3. 对账规则引擎
-- ============================================================================

-- 对账规则配置
CREATE TABLE recon_rule_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    rule_name       VARCHAR(200)  NOT NULL COMMENT '规则名称',
    rule_code       VARCHAR(50)   NOT NULL COMMENT '规则编码',
    rule_type       VARCHAR(30)   NOT NULL COMMENT '类型: EXACT_MATCH/RULE_MATCH/AI_SEMANTIC/AI_SPLIT',
    template_type   VARCHAR(50)   COMMENT '预置模板: BANK_STANDARD/BANK_STRICT/THIRD_PAYMENT/AR/AP/CROSS_SYSTEM/INTERCOMPANY',
    match_config    JSON          NOT NULL COMMENT '匹配规则配置',
    priority        INT           DEFAULT 0 COMMENT '优先级(数字越大越优先)',
    tolerance       JSON          COMMENT '容差配置: {amount_abs, amount_pct, date_days, similarity}',
    ai_config       JSON          COMMENT 'AI匹配配置',
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    description     TEXT          COMMENT '规则描述',
    version         INT           DEFAULT 1 COMMENT '版本号',
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    INDEX idx_rule_type (rule_type),
    UNIQUE KEY uk_rule_code (org_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账规则配置表';

-- 规则执行日志
CREATE TABLE recon_rule_exec_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT        NOT NULL COMMENT '规则ID',
    task_id         BIGINT        NOT NULL COMMENT '任务ID',
    input_count     INT           DEFAULT 0,
    matched_count   INT           DEFAULT 0,
    unmatched_count INT           DEFAULT 0,
    duration_ms     BIGINT        COMMENT '耗时(毫秒)',
    error_msg       TEXT,
    executed_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule_id (rule_id),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则执行日志表';

-- ============================================================================
-- 4. 对账任务与匹配结果
-- ============================================================================

-- 对账任务
CREATE TABLE recon_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    ledger_id       BIGINT        COMMENT '账套ID',
    task_name       VARCHAR(200)  NOT NULL COMMENT '任务名称',
    task_type       VARCHAR(30)   COMMENT '类型: BANK/THIRD_PAYMENT/AR/AP/CROSS_SYSTEM/INTERCOMPANY',
    source_a_id     BIGINT        NOT NULL COMMENT '数据源A',
    source_b_id     BIGINT        NOT NULL COMMENT '数据源B',
    rule_ids        JSON          COMMENT '关联规则ID列表',
    recon_period    VARCHAR(20)   COMMENT '对账期间(2024-07)',
    period_start    DATE          COMMENT '期间开始',
    period_end      DATE          COMMENT '期间结束',
    status          VARCHAR(20)   DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    total_a_count   INT           DEFAULT 0 COMMENT '数据源A总条数',
    total_b_count   INT           DEFAULT 0 COMMENT '数据源B总条数',
    matched_count   INT           DEFAULT 0 COMMENT '匹配成功条数',
    unmatched_count INT           DEFAULT 0 COMMENT '未匹配条数',
    discrepancy_count INT         DEFAULT 0 COMMENT '差异条数',
    match_rate      DECIMAL(5,2)  COMMENT '匹配率',
    match_summary   JSON          COMMENT '匹配汇总统计',
    error_msg       TEXT          COMMENT '错误信息',
    priority        VARCHAR(10)   DEFAULT 'NORMAL' COMMENT '优先级: LOW/NORMAL/HIGH/URGENT',
    started_at      DATETIME,
    completed_at    DATETIME,
    duration_ms     BIGINT,
    created_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    INDEX idx_status (status),
    INDEX idx_recon_period (recon_period),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账任务表';

-- 对账匹配结果
CREATE TABLE recon_match (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT        NOT NULL COMMENT '任务ID',
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    record_a_id     BIGINT        COMMENT '数据源A记录ID',
    record_b_id     BIGINT        COMMENT '数据源B记录ID',
    match_type      VARCHAR(30)   NOT NULL COMMENT '匹配类型: EXACT/RULE/AI_SEMANTIC/AI_SPLIT/MANUAL',
    confidence      DECIMAL(5,2)  COMMENT '置信度 0.00-100.00',
    match_dimensions JSON         COMMENT '各维度打分详情: {amount, date, party, desc, ref}',
    ai_explanation  TEXT          COMMENT 'AI匹配解释',
    amount_a        DECIMAL(18,2) COMMENT 'A方金额',
    amount_b        DECIMAL(18,2) COMMENT 'B方金额',
    amount_diff     DECIMAL(18,2) COMMENT '金额差异',
    date_diff_days  INT           COMMENT '日期差异天数',
    status          VARCHAR(20)   DEFAULT 'AUTO_CONFIRMED' COMMENT 'AUTO_CONFIRMED/PENDING_REVIEW/MANUAL_CONFIRMED/REJECTED',
    reviewed_by     BIGINT        COMMENT '审核人',
    reviewed_at     DATETIME      COMMENT '审核时间',
    review_comment  VARCHAR(500)  COMMENT '审核备注',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_match_type (match_type),
    INDEX idx_confidence (confidence),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账匹配结果表';

-- ============================================================================
-- 5. 差异管理中心
-- ============================================================================

-- 差异记录
CREATE TABLE recon_discrepancy (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT        NOT NULL COMMENT '任务ID',
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    record_id       BIGINT        COMMENT '未匹配的记录ID',
    related_record_id BIGINT      COMMENT '关联的候选记录ID',
    side            VARCHAR(10)   COMMENT '来源方: SOURCE_A/SOURCE_B',
    category        VARCHAR(50)   COMMENT '差异类别: TIME_DIFF/FEE_DIFF/EXCHANGE_DIFF/HUMAN_ERROR/UNREACHED/DUPLICATE/UNKNOWN',
    ai_root_cause   TEXT          COMMENT 'AI根因分析',
    ai_suggestion   JSON          COMMENT 'AI处理建议',
    amount          DECIMAL(18,2) COMMENT '本方金额',
    amount_diff     DECIMAL(18,2) COMMENT '差异金额',
    currency        VARCHAR(3)    DEFAULT 'CNY',
    risk_level      VARCHAR(10)   DEFAULT 'LOW' COMMENT '风险等级: LOW/MEDIUM/HIGH/CRITICAL',
    handler_id      BIGINT        COMMENT '指定处理人',
    handler_name    VARCHAR(100)  COMMENT '处理人姓名',
    status          VARCHAR(20)   DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/CLOSED',
    resolution      VARCHAR(50)   COMMENT '处理方式: MANUAL_MATCH/ADJUSTMENT/WRITE_OFF/IGNORE/ESCALATE',
    resolution_note TEXT          COMMENT '处理说明',
    resolved_by     BIGINT        COMMENT '处理人',
    resolved_at     DATETIME      COMMENT '处理时间',
    sla_deadline    DATETIME      COMMENT 'SLA截止时间',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_task_id (task_id),
    INDEX idx_category (category),
    INDEX idx_risk_level (risk_level),
    INDEX idx_status (status),
    INDEX idx_handler_id (handler_id),
    INDEX idx_sla_deadline (sla_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='差异记录表';

-- 调整分录
CREATE TABLE recon_adjustment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    discrepancy_id  BIGINT        COMMENT '关联差异ID',
    ledger_id       BIGINT        NOT NULL,
    adjustment_type VARCHAR(30)   COMMENT '调整类型: ACCRUAL/AMORTIZATION/EXCHANGE/FEE/WRITE_OFF/OTHER',
    amount          DECIMAL(18,2) NOT NULL,
    currency        VARCHAR(3)    DEFAULT 'CNY',
    debit_account   VARCHAR(50)   COMMENT '借方科目',
    credit_account  VARCHAR(50)   COMMENT '贷方科目',
    description     TEXT          COMMENT '调整说明',
    attachment_urls JSON          COMMENT '附件URL列表',
    status          VARCHAR(20)   DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/POSTED/REVERSED',
    approved_by     BIGINT,
    approved_at     DATETIME,
    created_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_discrepancy_id (discrepancy_id),
    INDEX idx_ledger_id (ledger_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调整分录表';

-- ============================================================================
-- 6. 审批工作流
-- ============================================================================

-- 审批流程定义
CREATE TABLE wf_process_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    process_name    VARCHAR(200)  NOT NULL,
    process_key     VARCHAR(50)   NOT NULL COMMENT '流程标识',
    process_type    VARCHAR(30)   COMMENT '类型: DISCREPANCY/ADJUSTMENT/EXPORT',
    bpmn_xml        LONGTEXT      COMMENT 'BPMN流程定义XML',
    version         INT           DEFAULT 1,
    status          VARCHAR(20)   DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED',
    created_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    UNIQUE KEY uk_process_key (org_id, process_key, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程定义表';

-- 审批记录
CREATE TABLE wf_approval_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    process_def_id  BIGINT        NOT NULL,
    business_type   VARCHAR(30)   COMMENT '业务类型: DISCREPANCY/ADJUSTMENT',
    business_id     BIGINT        COMMENT '业务ID',
    node_name       VARCHAR(200)  COMMENT '当前审批节点',
    approver_id     BIGINT        COMMENT '审批人ID',
    approver_name   VARCHAR(100)  COMMENT '审批人姓名',
    action          VARCHAR(20)   COMMENT '审批动作: APPROVE/REJECT/RETURN/DELEGATE',
    comment         TEXT          COMMENT '审批意见',
    attachments     JSON          COMMENT '附件',
    status          VARCHAR(20)   COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    sla_deadline    DATETIME      COMMENT 'SLA截止时间',
    approved_at     DATETIME      COMMENT '审批时间',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- ============================================================================
-- 7. 系统管理
-- ============================================================================

-- 用户
CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    username        VARCHAR(50)   NOT NULL COMMENT '用户名',
    password        VARCHAR(200)  NOT NULL COMMENT '密码(BCrypt)',
    real_name       VARCHAR(100)  COMMENT '真实姓名',
    email           VARCHAR(100)  COMMENT '邮箱',
    phone           VARCHAR(30)   COMMENT '手机号',
    avatar_url      VARCHAR(500)  COMMENT '头像URL',
    status          VARCHAR(20)   DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/LOCKED',
    last_login_at   DATETIME      COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)   COMMENT '最后登录IP',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_org_id (org_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色
CREATE TABLE sys_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    role_name       VARCHAR(100)  NOT NULL,
    role_code       VARCHAR(50)   NOT NULL,
    role_type       VARCHAR(20)   DEFAULT 'CUSTOM' COMMENT '类型: SYSTEM/CUSTOM',
    description     VARCHAR(500),
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    UNIQUE KEY uk_role_code (org_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限
CREATE TABLE sys_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT        DEFAULT 0,
    perm_name       VARCHAR(100)  NOT NULL,
    perm_code       VARCHAR(100)  NOT NULL,
    perm_type       VARCHAR(20)   COMMENT '类型: MENU/BUTTON/API',
    path            VARCHAR(200)  COMMENT '路由/API路径',
    method          VARCHAR(10)   COMMENT 'HTTP方法',
    icon            VARCHAR(100)  COMMENT '图标',
    sort_order      INT           DEFAULT 0,
    status          VARCHAR(20)   DEFAULT 'ACTIVE',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联
CREATE TABLE sys_user_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    role_id         BIGINT        NOT NULL,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联
CREATE TABLE sys_role_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         BIGINT        NOT NULL,
    permission_id   BIGINT        NOT NULL,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 审计日志
CREATE TABLE sys_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    user_id         BIGINT        COMMENT '操作人ID',
    username        VARCHAR(50)   COMMENT '操作人用户名',
    operation       VARCHAR(50)   COMMENT '操作类型: CREATE/UPDATE/DELETE/EXPORT/LOGIN/APPROVE',
    module          VARCHAR(50)   COMMENT '模块: DATASOURCE/RULE/ENGINE/DISCREPANCY/SYSTEM',
    target_type     VARCHAR(50)   COMMENT '目标类型',
    target_id       VARCHAR(100)  COMMENT '目标ID',
    request_url     VARCHAR(500)  COMMENT '请求URL',
    request_method  VARCHAR(10)   COMMENT '请求方法',
    request_params  JSON          COMMENT '请求参数(脱敏)',
    request_body    JSON          COMMENT '请求体(脱敏)',
    response_status INT           COMMENT '响应状态码',
    client_ip       VARCHAR(50)   COMMENT '客户端IP',
    user_agent      VARCHAR(500)  COMMENT 'User-Agent',
    duration_ms     BIGINT        COMMENT '执行耗时',
    detail          TEXT          COMMENT '操作详情',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- AI推理日志
CREATE TABLE sys_ai_inference_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    module          VARCHAR(50)   COMMENT '模块: FIELD_MAPPING/SEMANTIC_MATCH/ROOT_CAUSE/NL_QUERY/REPORT',
    input_data      JSON          COMMENT '输入数据',
    output_data     JSON          COMMENT '输出数据',
    model_used      VARCHAR(100)  COMMENT '使用的模型',
    tokens_used     INT           COMMENT '消耗Token数',
    latency_ms      INT           COMMENT '延迟(毫秒)',
    confidence      DECIMAL(5,2)  COMMENT '置信度',
    cost_amount     DECIMAL(10,6) COMMENT '调用成本',
    user_feedback   VARCHAR(20)   COMMENT '用户反馈: ACCEPTED/REJECTED/MODIFIED',
    feedback_note   TEXT          COMMENT '反馈备注',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推理日志表';

-- 通知记录
CREATE TABLE sys_notification (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL,
    user_id         BIGINT        NOT NULL,
    notify_type     VARCHAR(30)   COMMENT '类型: DISCREPANCY_ALERT/APPROVAL/SLA_WARNING/SYSTEM',
    title           VARCHAR(200)  NOT NULL,
    content         TEXT          NOT NULL,
    channel         VARCHAR(30)   COMMENT '渠道: IN_APP/EMAIL/SMS/WECOM/DINGTALK/FEISHU',
    is_read         TINYINT       DEFAULT 0,
    read_at         DATETIME,
    related_type    VARCHAR(30)   COMMENT '关联业务类型',
    related_id      BIGINT        COMMENT '关联业务ID',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知记录表';

-- ============================================================================
-- 8. 许可证管理
-- ============================================================================

-- 系统许可证
CREATE TABLE sys_license (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id          BIGINT        NOT NULL COMMENT '组织ID',
    license_data    TEXT          NOT NULL COMMENT '加密的许可证数据',
    org_name        VARCHAR(200)  COMMENT '授权组织名称',
    expire_date     DATE          COMMENT '到期日期',
    max_users       INT           DEFAULT 0 COMMENT '最大用户数',
    features        JSON          COMMENT '功能授权列表',
    machine_id      VARCHAR(200)  COMMENT '机器指纹',
    status          VARCHAR(20)   DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/EXPIRED/REVOKED',
    activated_at    DATETIME      COMMENT '激活时间',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统许可证表';

-- ============================================================================
-- 9. 初始化数据
-- ============================================================================

-- 默认组织
INSERT INTO org_organization (id, org_name, org_code, org_type, status) VALUES
(1, '默认公司', 'DEFAULT_CO', 'COMPANY', 'ACTIVE');

-- 默认账套
INSERT INTO org_ledger (id, org_id, ledger_name, ledger_code, currency, accounting_std) VALUES
(1, 1, '默认账套', 'DEFAULT_LEDGER', 'CNY', 'CAS');

-- 默认管理员用户 (密码: admin123)
INSERT INTO sys_user (id, org_id, username, password, real_name, email, status) VALUES
(1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 'admin@recon.com', 'ACTIVE');

-- 预置角色
INSERT INTO sys_role (id, org_id, role_name, role_code, role_type) VALUES
(1, 1, '超级管理员', 'SUPER_ADMIN', 'SYSTEM'),
(2, 1, '财务总监', 'CFO', 'SYSTEM'),
(3, 1, '财务主管', 'FINANCE_MANAGER', 'SYSTEM'),
(4, 1, '会计', 'ACCOUNTANT', 'SYSTEM'),
(5, 1, '审计师(只读)', 'AUDITOR', 'SYSTEM');

-- 管理员角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 预置系统权限
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path) VALUES
(1, 0, '数据源管理', 'datasource', 'MENU', '/datasource'),
(2, 0, '规则引擎', 'rule', 'MENU', '/rule'),
(3, 0, '对账工作台', 'workbench', 'MENU', '/workbench'),
(4, 0, '差异管理', 'discrepancy', 'MENU', '/discrepancy'),
(5, 0, '智能分析', 'analytics', 'MENU', '/analytics'),
(6, 0, '审批中心', 'workflow', 'MENU', '/workflow'),
(7, 0, '系统管理', 'system', 'MENU', '/system'),
(8, 0, '开放平台', 'platform', 'MENU', '/platform');

-- 预置对账规则模板
INSERT INTO recon_rule_config (id, org_id, rule_name, rule_code, rule_type, template_type, match_config, tolerance, priority) VALUES
(1, 1, '银行对账-标准', 'BANK_STANDARD', 'RULE_MATCH', 'BANK_STANDARD',
 '{"conditions":[{"field":"amount","operator":"eq"},{"field":"transaction_date","operator":"date_range","value":1},{"field":"description","operator":"fuzzy_match","threshold":0.8}]}',
 '{"amount_abs":0,"amount_pct":0,"date_days":1}', 10),
(2, 1, '银行对账-严格', 'BANK_STRICT', 'EXACT_MATCH', 'BANK_STRICT',
 '{"conditions":[{"field":"amount","operator":"eq"},{"field":"transaction_ref","operator":"eq"},{"field":"transaction_date","operator":"eq"}]}',
 '{"amount_abs":0,"amount_pct":0,"date_days":0}', 20),
(3, 1, '三方支付对账', 'THIRD_PAYMENT', 'RULE_MATCH', 'THIRD_PAYMENT',
 '{"conditions":[{"field":"amount","operator":"eq"},{"field":"order_no","operator":"eq"}]}',
 '{"amount_abs":1,"amount_pct":0.005,"date_days":2}', 10),
(4, 1, 'AI语义匹配', 'AI_SEMANTIC', 'AI_SEMANTIC', NULL,
 '{"embedding_model":"bge-m3","llm_model":"gpt-4o","dimensions":["amount","date","party","description","reference"]}',
 '{"embedding_threshold":0.75,"llm_auto_threshold":0.85,"llm_recommend_threshold":0.70}', 5);
