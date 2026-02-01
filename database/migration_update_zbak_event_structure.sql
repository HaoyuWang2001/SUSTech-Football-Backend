-- 更新备份表 zbak_event 的结构，添加新字段
-- 修复删除赛事时的 INSERT 字段数不匹配错误

-- 给 zbak_event 表添加 match_player_count 字段
ALTER TABLE zbak_event
    ADD COLUMN IF NOT EXISTS match_player_count INT;

-- 给 zbak_event 表添加 roster_size 字段
ALTER TABLE zbak_event
    ADD COLUMN IF NOT EXISTS roster_size INT;

-- 验证字段已添加
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'zbak_event' 
ORDER BY ordinal_position;
