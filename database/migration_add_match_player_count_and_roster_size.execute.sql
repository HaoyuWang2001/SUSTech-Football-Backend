
ALTER TABLE event ADD COLUMN IF NOT EXISTS match_player_count INT;

ALTER TABLE event ADD COLUMN IF NOT EXISTS roster_size INT;

SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'event' 
AND column_name IN ('match_player_count', 'roster_size');
