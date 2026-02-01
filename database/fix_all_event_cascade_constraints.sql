-- 修复所有与 event 表相关的外键约束，添加 ON DELETE CASCADE
-- 执行此脚本后，删除赛事时会自动级联删除所有相关数据

-- ============================================
-- 1. event_manager 表
-- ============================================
-- 删除旧的外键约束
ALTER TABLE event_manager
    DROP CONSTRAINT IF EXISTS event_manager_event_id_fkey;

-- 添加新的带级联删除的外键约束
ALTER TABLE event_manager
    ADD CONSTRAINT event_manager_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 2. event_group 表
-- ============================================
ALTER TABLE event_group
    DROP CONSTRAINT IF EXISTS event_group_event_id_fkey;

ALTER TABLE event_group
    ADD CONSTRAINT event_group_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 3. event_referee 表
-- ============================================
ALTER TABLE event_referee
    DROP CONSTRAINT IF EXISTS event_referee_event_id_fkey;

ALTER TABLE event_referee
    ADD CONSTRAINT event_referee_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 4. event_referee_request 表
-- ============================================
ALTER TABLE event_referee_request
    DROP CONSTRAINT IF EXISTS event_referee_request_event_id_fkey;

ALTER TABLE event_referee_request
    ADD CONSTRAINT event_referee_request_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 5. event_stage 表
-- ============================================
ALTER TABLE event_stage
    DROP CONSTRAINT IF EXISTS event_stage_event_id_fkey;

ALTER TABLE event_stage
    ADD CONSTRAINT event_stage_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 6. event_stage_tag 表
-- ============================================
ALTER TABLE event_stage_tag
    DROP CONSTRAINT IF EXISTS event_stage_tag_event_id_fkey;

ALTER TABLE event_stage_tag
    ADD CONSTRAINT event_stage_tag_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 7. event_match 表
-- ============================================
ALTER TABLE event_match
    DROP CONSTRAINT IF EXISTS event_match_event_id_fkey;

ALTER TABLE event_match
    ADD CONSTRAINT event_match_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 8. favorite_event 表
-- ============================================
ALTER TABLE favorite_event
    DROP CONSTRAINT IF EXISTS favorite_event_event_id_fkey;

ALTER TABLE favorite_event
    ADD CONSTRAINT favorite_event_event_id_fkey 
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

-- ============================================
-- 验证修改结果
-- ============================================
SELECT 
    tc.table_name, 
    kcu.column_name,
    tc.constraint_name,
    rc.delete_rule
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.referential_constraints rc 
    ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND kcu.column_name = 'event_id'
ORDER BY tc.table_name;
