
CREATE TABLE IF NOT EXISTS zbak_event_team_roster
(
    LIKE event_team_roster INCLUDING ALL
);

DROP FUNCTION IF EXISTS backup_and_delete_event() CASCADE;

CREATE OR REPLACE FUNCTION backup_and_delete_event()
    RETURNS TRIGGER AS
$$
BEGIN
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

    RETURN OLD;
END;
$$
    LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_backup_and_delete ON event;

CREATE TRIGGER trigger_backup_and_delete
    BEFORE DELETE
    ON event
    FOR EACH ROW
EXECUTE FUNCTION backup_and_delete_event();

SELECT 
    tablename, 
    schemaname 
FROM pg_tables 
WHERE tablename IN ('zbak_event_team_roster')
ORDER BY tablename;

SELECT 
    trigger_name,
    event_manipulation,
    event_object_table,
    action_statement
FROM information_schema.triggers
WHERE trigger_name = 'trigger_backup_and_delete';

SELECT 
    routine_name,
    routine_type,
    data_type
FROM information_schema.routines
WHERE routine_name = 'backup_and_delete_event';
