
ALTER TABLE event_team_roster 
DROP CONSTRAINT IF EXISTS event_team_roster_event_id_team_id_fkey;

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

ALTER TABLE event_team DROP CONSTRAINT IF EXISTS event_team_event_id_fkey;
ALTER TABLE event_team DROP CONSTRAINT IF EXISTS event_team_team_id_fkey;

ALTER TABLE event_team 
ADD CONSTRAINT event_team_event_id_fkey 
FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE;

ALTER TABLE event_team 
ADD CONSTRAINT event_team_team_id_fkey 
FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE;

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

ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_event_id_fkey;
ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_team_id_fkey;
ALTER TABLE event_team_roster DROP CONSTRAINT IF EXISTS event_team_roster_player_id_fkey;

ALTER TABLE event_team_roster 
ADD CONSTRAINT event_team_roster_event_id_team_id_fkey 
FOREIGN KEY (event_id, team_id) REFERENCES event_team(event_id, team_id) ON DELETE CASCADE;

ALTER TABLE event_team_roster 
ADD CONSTRAINT event_team_roster_player_id_fkey 
FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE;

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
