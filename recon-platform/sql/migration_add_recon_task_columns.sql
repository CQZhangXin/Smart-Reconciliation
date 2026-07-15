-- ============================================================================
-- Migration: 补充实体中存在但数据库表中缺失的字段（共7列，3张表）
-- 日期: 2026-07-13
-- 执行方式: MySQL Workbench 或命令行 mysql -u root -p < 本文件
-- ============================================================================

USE recondb;

-- 1. org_organization 表缺失 org_id
ALTER TABLE org_organization
    ADD COLUMN org_id BIGINT COMMENT '组织ID（租户隔离）' AFTER id;

-- 2. recon_adjustment 表缺失 effective_date, posting_date
ALTER TABLE recon_adjustment
    ADD COLUMN effective_date DATE     COMMENT '生效日期' AFTER credit_account,
    ADD COLUMN posting_date   DATETIME COMMENT '过账日期' AFTER effective_date;

-- 3. recon_task 表缺失 updated_by, scheduled_at, retry_count, parent_task_id
ALTER TABLE recon_task
    ADD COLUMN updated_by     BIGINT   COMMENT '更新人'          AFTER created_by,
    ADD COLUMN scheduled_at   DATETIME COMMENT '计划执行时间'     AFTER updated_by,
    ADD COLUMN retry_count    INT      DEFAULT 0 COMMENT '重试次数' AFTER scheduled_at,
    ADD COLUMN parent_task_id BIGINT   COMMENT '父任务ID'        AFTER retry_count;
