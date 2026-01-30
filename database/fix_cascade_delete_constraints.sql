-- 数据库修复脚本：修改外键约束以支持级联删除
-- 创建日期：2026-01-31
-- 问题：删除赛事失败，因为 event_team_roster 表的外键约束阻止删除

-- ============================================
-- 修复 event_team 表的外键约束
-- ============================================

-- 1. 先删除 event_team_roster 的外键约束（因为它依赖 event_team）
ALTER TABLE event_team_roster 
DROP CONSTRAINT IF EXISTS event_team_roster_event_id_team_id_fkey;

-- 2. 重新创建 event_team 表（如果有外键约束问题）
-- 注意：如果表中已有数据，需要先备份

-- 查看当前的外键约束
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.table_name = 'event_team' AND tc.constraint_type = 'FOREIGN KEY';

-- 删除旧的外键约束（根据上面查询的结果调整约束名）
ALTER TABLE event_team DROP CONSTRAINT IF EXISTS event_team_event_id_fkey;
ALTER TABLE event_team DROP CONSTRAINT IF EXISTS event_team_team_id_fkey;

-- 添加新的级联删除外键约束
ALTER TABLE event_team 
ADD CONSTRAINT event_team_event_id_fkey 
FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

ALTER TABLE event_team 
ADD CONSTRAINT event_team_team_id_fkey 
FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE;

-- ============================================
-- 修复 event_team_roster 表的外键约束
-- ============================================

-- 查看当前的外键约束
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.table_name = 'event_team_roster' AND tc.constraint_type = 'FOREIGN KEY';

-- 删除旧的外键约束
ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_event_id_fkey;
ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_team_id_fkey;
ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_player_id_fkey;

-- 添加新的级联删除外键约束
ALTER TABLE event_team_roster 
ADD CONSTRAINT event_team_roster_event_id_team_id_fkey 
FOREIGN KEY (event_id, team_id) REFERENCES event_team(event_id, team_id) ON DELETE CASCADE;

ALTER TABLE event_team_roster 
ADD CONSTRAINT event_team_roster_player_id_fkey 
FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE;

-- ============================================
-- 验证修复结果
-- ============================================

-- 查看 event_team 的外键约束
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
  ON tc.constraint_name = rc.constraint_name
WHERE tc.table_name = 'event_team' AND tc.constraint_type = 'FOREIGN KEY';

-- 查看 event_team_roster 的外键约束
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
  ON tc.constraint_name = rc.constraint_name
WHERE tc.table_name = 'event_team_roster' AND tc.constraint_type = 'FOREIGN KEY';

-- 测试删除功能（可选，谨慎使用）
-- DELETE FROM event WHERE event_id = <test_event_id>;
