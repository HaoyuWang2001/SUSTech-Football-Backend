-- 数据库迁移脚本：为event表增加比赛人数和大名单人数字段
-- 创建日期：2026-01-30

-- 为event表添加match_player_count字段（比赛人数：5人制、7人制、8人制、11人制）
ALTER TABLE event ADD COLUMN IF NOT EXISTS match_player_count INT;

-- 为event表添加roster_size字段（大名单人数：每支参赛球队可报名的球员总数）
ALTER TABLE event ADD COLUMN IF NOT EXISTS roster_size INT;

-- 为字段添加注释
COMMENT ON COLUMN event.match_player_count IS '比赛人数：5人制、7人制、8人制、11人制';
COMMENT ON COLUMN event.roster_size IS '大名单人数：每支参赛球队可报名的球员总数';

-- 验证脚本执行结果
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'event' 
AND column_name IN ('match_player_count', 'roster_size');
