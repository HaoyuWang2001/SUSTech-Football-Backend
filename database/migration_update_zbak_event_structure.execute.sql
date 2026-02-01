
ALTER TABLE zbak_event
    ADD COLUMN IF NOT EXISTS match_player_count INT;

ALTER TABLE zbak_event
    ADD COLUMN IF NOT EXISTS roster_size INT;

SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'zbak_event' 
ORDER BY ordinal_position;
