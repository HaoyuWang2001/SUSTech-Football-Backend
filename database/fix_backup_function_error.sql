-- 数据库修复脚本：修复赛事删除时的备份函数错误
-- 创建日期：2026-01-31
-- 问题：删除赛事时备份函数报错 "INSERT 的表达式多于指定的字段数"
-- 原因：缺少 zbak_event_team_roster 备份表，以及备份/删除函数中缺少相关逻辑

-- ============================================
-- 1. 创建缺失的备份表
-- ============================================

-- 创建 event_team_roster 的备份表（如果不存在）
CREATE TABLE IF NOT EXISTS zbak_event_team_roster
(
    LIKE event_team_roster INCLUDING ALL
);

COMMENT ON TABLE zbak_event_team_roster IS '赛事球队大名单备份表';

-- ============================================
-- 2. 更新备份和删除函数
-- ============================================

-- 删除旧的函数
DROP FUNCTION IF EXISTS backup_and_delete_event() CASCADE;

-- 重新创建函数，包含 event_team_roster 的处理
CREATE OR REPLACE FUNCTION backup_and_delete_event()
    RETURNS TRIGGER AS
$$
BEGIN
    -- 开始复制
    INSERT INTO zbak_event
    SELECT *
    FROM event
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_manager
    SELECT *
    FROM event_manager
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_team
    SELECT *
    FROM event_team
    WHERE event_id = OLD.event_id;

    -- 备份大名单数据（新增）
    INSERT INTO zbak_event_team_roster
    SELECT *
    FROM event_team_roster
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_group
    SELECT *
    FROM event_group
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_group_team
    SELECT event_group_team.*
    FROM event_group_team
    WHERE event_group_team.group_id IN (SELECT group_id
                                        FROM event_group
                                        WHERE event_id = OLD.event_id);

    INSERT INTO zbak_event_team_request
    SELECT *
    FROM event_team_request
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_referee
    SELECT *
    FROM event_referee
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_referee_request
    SELECT *
    FROM event_referee_request
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_stage
    SELECT *
    FROM event_stage
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_stage_tag
    SELECT *
    FROM event_stage_tag
    WHERE event_id = OLD.event_id;

    INSERT INTO zbak_event_match
    SELECT *
    FROM event_match
    WHERE event_id = OLD.event_id;

    -- 开始删除
    DELETE
    FROM event_match
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_stage_tag
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_stage
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_referee_request
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_referee
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_team_request
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_group_team
    WHERE event_group_team.group_id IN (SELECT group_id
                                        FROM event_group
                                        WHERE event_id = OLD.event_id);

    DELETE
    FROM event_group
    WHERE event_id = OLD.event_id;

    -- 删除大名单数据（新增，必须在删除 event_team 之前）
    DELETE
    FROM event_team_roster
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_team
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_manager
    WHERE event_id = OLD.event_id;

    DELETE
    FROM favorite_event
    WHERE event_id = OLD.event_id;

    -- 返回OLD以允许删除操作继续
    RETURN OLD;
END;
$$
    LANGUAGE plpgsql;

-- 重新创建触发器
DROP TRIGGER IF EXISTS trigger_backup_and_delete ON event;

CREATE TRIGGER trigger_backup_and_delete
    BEFORE DELETE
    ON event
    FOR EACH ROW
EXECUTE FUNCTION backup_and_delete_event();

-- ============================================
-- 3. 验证修复结果
-- ============================================

-- 检查备份表是否存在
SELECT 
    tablename, 
    schemaname 
FROM pg_tables 
WHERE tablename IN ('zbak_event_team_roster')
ORDER BY tablename;

-- 检查触发器是否正确创建
SELECT 
    trigger_name,
    event_manipulation,
    event_object_table,
    action_statement
FROM information_schema.triggers
WHERE trigger_name = 'trigger_backup_and_delete';

-- 检查函数是否存在
SELECT 
    routine_name,
    routine_type,
    data_type
FROM information_schema.routines
WHERE routine_name = 'backup_and_delete_event';

-- ============================================
-- 完成提示
-- ============================================

SELECT '修复完成！现在可以正常删除赛事了。' AS status;
