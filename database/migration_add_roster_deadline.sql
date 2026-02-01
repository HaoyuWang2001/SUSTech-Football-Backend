-- 迁移脚本：为 event 表添加大名单截止日期字段
-- 执行时间：2026-02-01
-- 说明：添加 roster_deadline 字段，用于设置球队提交大名单的截止时间

-- 添加 roster_deadline 字段到 event 表
ALTER TABLE event
    ADD COLUMN IF NOT EXISTS roster_deadline TIMESTAMP;

-- 添加字段注释
COMMENT ON COLUMN event.roster_deadline IS '大名单截止日期：球队需在此时间前提交大名单';

-- 同步更新备份表 zbak_event，添加相同的字段
ALTER TABLE zbak_event
    ADD COLUMN IF NOT EXISTS roster_deadline TIMESTAMP;
