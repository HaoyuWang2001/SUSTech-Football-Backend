-- 数据库迁移脚本：添加赛事球队大名单表
-- 创建日期：2026-01-31

-- 创建赛事球队大名单表
CREATE TABLE IF NOT EXISTS event_team_roster
(
    event_id  INT REFERENCES event (event_id),
    team_id   INT REFERENCES team (team_id),
    player_id INT REFERENCES player (player_id),
    number    INT, -- 球衣号码
    PRIMARY KEY (event_id, team_id, player_id),
    FOREIGN KEY (event_id, team_id) REFERENCES event_team (event_id, team_id)
);

-- 为字段添加注释
COMMENT ON TABLE event_team_roster IS '赛事球队大名单表，存储每个球队在赛事中的参赛球员';
COMMENT ON COLUMN event_team_roster.event_id IS '赛事ID';
COMMENT ON COLUMN event_team_roster.team_id IS '球队ID';
COMMENT ON COLUMN event_team_roster.player_id IS '球员ID';
COMMENT ON COLUMN event_team_roster.number IS '球员在该赛事中的球衣号码';

-- 创建索引以提高查询效率
CREATE INDEX IF NOT EXISTS idx_event_team_roster_event ON event_team_roster(event_id);
CREATE INDEX IF NOT EXISTS idx_event_team_roster_team ON event_team_roster(team_id);
CREATE INDEX IF NOT EXISTS idx_event_team_roster_player ON event_team_roster(player_id);

-- 验证脚本执行结果
SELECT 
    table_name, 
    column_name, 
    data_type, 
    is_nullable 
FROM information_schema.columns 
WHERE table_name = 'event_team_roster'
ORDER BY ordinal_position;
