
CREATE TABLE IF NOT EXISTS event_team_roster
(
    event_id  INT REFERENCES event (event_id),
    team_id   INT REFERENCES team (team_id),
    player_id INT REFERENCES player (player_id),
    number    INT,
    PRIMARY KEY (event_id, team_id, player_id),
    FOREIGN KEY (event_id, team_id) REFERENCES event_team (event_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_event_team_roster_event ON event_team_roster(event_id);
CREATE INDEX IF NOT EXISTS idx_event_team_roster_team ON event_team_roster(team_id);
CREATE INDEX IF NOT EXISTS idx_event_team_roster_player ON event_team_roster(player_id);

SELECT 
    table_name, 
    column_name, 
    data_type, 
    is_nullable 
FROM information_schema.columns 
WHERE table_name = 'event_team_roster'
ORDER BY ordinal_position;
