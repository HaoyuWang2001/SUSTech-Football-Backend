-- last_updated鍑芥暟
CREATE
    OR REPLACE FUNCTION update_last_updated_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.last_updated
        = transaction_timestamp();
    RETURN NEW;
END;
$$
    language 'plpgsql';


-- 鐢ㄦ埛琛?
CREATE TABLE t_user
(
    user_id     SERIAL PRIMARY KEY,
    openid      VARCHAR(255) NOT NULL UNIQUE,
    session_key VARCHAR(255) NOT NULL,
    nick_name   VARCHAR(255),
    avatar_url  VARCHAR(255)
);

-- 鐞冨憳琛?
CREATE TABLE player
(
    player_id      SERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    photo_url      VARCHAR(255),
    birth_date     DATE,
    height         INT,
    weight         INT,
    position       VARCHAR(100), -- 鍚庡崼銆佸墠閿嬬瓑
    identity       VARCHAR(100), -- 鏈鐢燂紝鐮旂┒鐢燂紝鏁欒亴宸ワ紝鍏朵粬
    shu_yuan       VARCHAR(100), -- 鑻ユ湰绉戠敓鍒欐湁涔﹂櫌
    college        VARCHAR(255), -- 鑻ユ湰绉戠敓/鐮旂┒鐢?/鏁欒亴宸ュ垯鏈夐櫌绯?
    admission_year INT,          -- 鍏ュ骞翠唤
    bio            TEXT,         -- 涓汉绠?浠?
    user_id        INT UNIQUE REFERENCES t_user
);

-- 鏁欑粌琛?
CREATE TABLE coach
(
    coach_id  SERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    photo_url VARCHAR(255),
    bio       TEXT,
    user_id   INT REFERENCES t_user
);

-- 瑁佸垽琛?
CREATE TABLE referee
(
    referee_id SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    photo_url  VARCHAR(255),
    bio        TEXT,
    user_id    INT REFERENCES t_user
);

-- 鐞冮槦琛?
CREATE TABLE team
(
    team_id     SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    logo_url    VARCHAR(255),
    captain_id  INT REFERENCES player (player_id),
    description TEXT
);

-- 鐞冮槦-闃熸湇
CREATE TABLE team_uniform
(
    team_id     INT REFERENCES team (team_id),
    uniform_url VARCHAR(255),
    PRIMARY KEY (team_id, uniform_url)
);

-- 鐞冮槦绠＄悊鑰呰〃
CREATE TABLE team_manager
(
    user_id  INT REFERENCES t_user,
    team_id  INT REFERENCES team,
    is_owner BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, team_id)
);

-- 鐞冮槦-鐞冨憳
CREATE TABLE team_player
(
    team_id   INT REFERENCES team (team_id),
    player_id INT REFERENCES player (player_id),
    number    INT DEFAULT 0, -- 鐞冭。鍙风爜
    PRIMARY KEY (team_id, player_id)
);

-- 鐞冮槦閭?璇风悆鍛?/鐞冨憳鐢宠鍔犲叆鐞冮槦
CREATE TABLE team_player_request
(
    team_id      INT REFERENCES team (team_id),
    player_id    INT REFERENCES player (player_id),
    type         VARCHAR CHECK ( type IN ('INVITATION', 'APPLICATION') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (team_id, player_id, type)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON team_player_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- 鐞冮槦-鏁欑粌
CREATE TABLE team_coach
(
    team_id  INT REFERENCES team (team_id),
    coach_id INT REFERENCES coach (coach_id),
    PRIMARY KEY (team_id, coach_id)
);

-- 鐞冮槦閭?璇锋暀缁?
CREATE TABLE team_coach_request
(
    team_id      INT REFERENCES team (team_id),
    coach_id     INT REFERENCES coach (coach_id),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (team_id, coach_id)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON team_coach_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 姣旇禌琛?
CREATE TABLE match
(
    match_id          SERIAL PRIMARY KEY,
    home_team_id      INT REFERENCES team (team_id),
    away_team_id      INT REFERENCES team (team_id),
    time              TIMESTAMP,
    home_team_score   INT                                                            DEFAULT 0,
    away_team_score   INT                                                            DEFAULT 0,
    home_team_penalty INT                                                            DEFAULT 0,
    away_team_penalty INT                                                            DEFAULT 0,
    status            VARCHAR CHECK ( status IN ('PENDING', 'ONGOING', 'FINISHED') ) DEFAULT 'PENDING',
    description       TEXT
);

CREATE TABLE match_manager
(
    match_id INT REFERENCES match,
    user_id  INT REFERENCES t_user,
    is_owner BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (match_id, user_id)
);

-- 姣旇禌(鍙嬭皧璧?)閭?璇风悆闃?
CREATE TABLE match_team_request
(
    match_id     INT REFERENCES match,
    team_id      INT REFERENCES team,
    type         VARCHAR CHECK ( type IN ('HOME', 'AWAY') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (match_id, team_id)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON match_team_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 姣旇禌-瑁佸垽
CREATE TABLE match_referee
(
    match_id   INT REFERENCES match,
    referee_id INT REFERENCES referee,
    PRIMARY KEY (match_id, referee_id)
);

-- 姣旇禌閭?璇疯鍒?
CREATE TABLE match_referee_request
(
    match_id     INT REFERENCES match,
    referee_id   INT REFERENCES referee,
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (match_id, referee_id)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON match_referee_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- 姣旇禌-鐞冨憳琛屼负锛堣繘鐞冦?佺孩鐗屻?侀粍鐗岋級
CREATE TABLE match_player_action
(
    match_id  INT REFERENCES match,
    team_id   INT REFERENCES team,
    player_id INT REFERENCES player,
    action    VARCHAR CHECK ( action IN ('GOAL', 'ASSIST', 'YELLOW_CARD', 'RED_CARD', 'ON', 'OFF')
        ),
    time      INTEGER, -- 姣旇禌寮?濮嬬殑鏃堕棿
    PRIMARY KEY (match_id, team_id, player_id, action, time)
);

CREATE TABLE match_live
(
    live_id   SERIAL PRIMARY KEY,
    match_id  INT REFERENCES match,
    live_name VARCHAR(255),
    live_url  VARCHAR(255)
);

CREATE TABLE match_video
(
    video_id   SERIAL PRIMARY KEY,
    match_id   INT REFERENCES match,
    video_name VARCHAR(255),
    video_url  VARCHAR(255)
);

CREATE TABLE match_player
(
    match_id  INT REFERENCES match,
    team_id   INT REFERENCES team,
    player_id INT REFERENCES player,
    number    INT,
    is_start  BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (match_id, team_id, player_id)
);

-- 璧涗簨琛?
CREATE TABLE event
(
    event_id           SERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    match_player_count INT, -- 姣旇禌浜烘暟锛?浜哄埗銆?浜哄埗銆?浜哄埗銆?1浜哄埗
    roster_size        INT  -- 澶у悕鍗曚汉鏁帮細姣忔敮鍙傝禌鐞冮槦鍙姤鍚嶇殑鐞冨憳鎬绘暟
);

-- 璧涗簨绠＄悊鑰呰〃
CREATE TABLE event_manager
(
    event_id INT REFERENCES event (event_id),
    user_id  INT REFERENCES t_user,
    is_owner BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (event_id, user_id)
);

-- 璧涗簨-鐞冮槦
CREATE TABLE event_team
(
    event_id INT REFERENCES event (event_id),
    team_id  INT REFERENCES team (team_id),
    PRIMARY KEY (event_id, team_id)
);

-- 灏忕粍
CREATE TABLE event_group
(
    group_id SERIAL PRIMARY KEY,
    event_id INT REFERENCES event (event_id),
    name     VARCHAR(255) NOT NULL
);

-- 灏忕粍-鐞冮槦
CREATE TABLE event_group_team
(
    group_id          INT REFERENCES event_group (group_id),
    team_id           INT REFERENCES team (team_id),
    num_wins          INT DEFAULT 0, -- 鑳滃満
    num_draws         INT DEFAULT 0, -- 骞冲眬
    num_losses        INT DEFAULT 0, -- 璐熷満
    num_goals_for     INT DEFAULT 0, -- 杩涚悆鏁?
    num_goals_against INT DEFAULT 0, -- 澶辩悆鏁?
    score             INT DEFAULT 0, -- 绉垎
    PRIMARY KEY (group_id, team_id)
);

-- 璧涗簨閭?璇风悆闃?/鐞冮槦鐢宠鍔犲叆璧涗簨
CREATE TABLE event_team_request
(
    event_id     INT REFERENCES event (event_id),
    team_id      INT REFERENCES team (team_id),
    type         VARCHAR CHECK ( type IN ('INVITATION', 'APPLICATION') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (event_id, team_id, type)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON event_team_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 璧涗簨-瑁佸垽
CREATE TABLE event_referee
(
    event_id   INT REFERENCES event,
    referee_id INT REFERENCES referee,
    PRIMARY KEY (event_id, referee_id)
);

-- 璧涗簨閭?璇疯鍒?
CREATE TABLE event_referee_request
(
    event_id     INT REFERENCES event,
    referee_id   INT REFERENCES referee,
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (event_id, referee_id)
);

-- 鏇存柊last_updated瑙﹀彂鍣?
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON event_referee_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 璧涗簨-姣旇禌闃舵锛氬皬缁勮禌銆佹窐姹拌禌銆佹帓浣嶈禌绛?
CREATE TABLE event_stage
(
    event_id INT REFERENCES event,
    stage    VARCHAR, -- 灏忕粍璧涖?佹窐姹拌禌銆佹帓浣嶈禌绛?
    PRIMARY KEY (event_id, stage)
);

-- 銆愯禌浜?-姣旇禌闃舵銆?-鏍囩锛氬stage=灏忕粍璧涳紝tag=A缁勩?丅缁勭瓑锛泂tage=娣樻卑璧涳紝tag=1/8鍐宠禌銆?1/4鍐宠禌绛?
CREATE TABLE event_stage_tag
(
    event_id INT REFERENCES event,
    stage    VARCHAR, -- 灏忕粍璧涖?佹窐姹拌禌銆佹帓浣嶈禌绛?
    tag      VARCHAR, -- A缁勩?丅缁勭瓑锛?1/8鍐宠禌銆?1/4鍐宠禌绛?
    FOREIGN KEY (event_id, stage) REFERENCES event_stage,
    PRIMARY KEY (event_id, stage, tag)
);

-- 璧涗簨-姣旇禌
CREATE TABLE event_match
(
    event_id INT REFERENCES event,
    match_id INT REFERENCES match,
    stage    VARCHAR,
    tag      VARCHAR,
    PROPERTY VARCHAR,
    PRIMARY KEY (event_id, match_id),
    FOREIGN KEY (event_id, stage) REFERENCES event_stage,
    FOREIGN KEY (event_id, stage, tag) REFERENCES event_stage_tag
);

-- 閫氱煡琛?
CREATE TABLE notification
(
    notification_id SERIAL PRIMARY KEY,
    publisher_id    INT REFERENCES t_user,
    type            VARCHAR CHECK ( type IN ('ALL_USERS',
                                             'TEAM_TO_PLAYER',
                                             'EVENT_TO_TEAM',
                                             'EVENT_TO_PLAYER',
                                             'MATCH_TO_TEAM',
                                             'MATCH_TO_PLAYER') ),
    source_id       INT,
    target_id       INT,
    content         TEXT,
    time            TIMESTAMP
);

-- 鏀惰棌鐢ㄦ埛琛?
CREATE TABLE favorite_user
(
    user_id     INT REFERENCES t_user (user_id),
    favorite_id INT REFERENCES t_user (user_id),
    PRIMARY KEY (user_id, favorite_id)
);

-- 鏀惰棌鐞冮槦琛?
CREATE TABLE favorite_team
(
    user_id INT REFERENCES t_user (user_id),
    team_id INT REFERENCES team (team_id),
    PRIMARY KEY (user_id, team_id)
);

-- 鏀惰棌璧涗簨琛?
CREATE TABLE favorite_event
(
    user_id  INT REFERENCES t_user (user_id),
    event_id INT REFERENCES event (event_id),
    PRIMARY KEY (user_id, event_id)
);

-- 鏀惰棌姣旇禌琛?
CREATE TABLE favorite_match
(
    user_id  INT REFERENCES t_user (user_id),
    match_id INT REFERENCES match (match_id),
    PRIMARY KEY (user_id, match_id)
);

-- 姣旇禌璇勮琛?
CREATE TABLE match_comment
(
    comment_id SERIAL PRIMARY KEY,
    user_id    INT REFERENCES t_user (user_id),
    match_id   INT REFERENCES match (match_id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    time       TIMESTAMP DEFAULT now()
);

-- 浜岀骇璇勮琛?
CREATE TABLE match_comment_reply
(
    reply_id   SERIAL PRIMARY KEY,
    user_id    INT REFERENCES t_user (user_id),
    comment_id INT REFERENCES match_comment (comment_id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    time       TIMESTAMP DEFAULT now()
);

-- 璇勮鐐硅禐琛?
CREATE TABLE match_comment_like
(
    user_id    INT REFERENCES t_user (user_id),
    comment_id INT REFERENCES match_comment (comment_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, comment_id)
);

CREATE TABLE wx_article
(
    article_id SERIAL PRIMARY KEY,
    url        VARCHAR(255) NOT NULL,
    cover_url  VARCHAR(255),
    title      VARCHAR(255),
    time       TIMESTAMP DEFAULT now()
);


-- 璇硶

-- CREATE FUNCTION function_name (鍙傛暟鍒楄〃)
-- RETURNS 杩斿洖绫诲瀷 AS $$
-- DECLARE
--     -- 鍙橀噺澹版槑
-- BEGIN
--     -- 鍑芥暟浣擄紝鎵ц鐨凷QL璇彞鎴栭?昏緫
--     RETURN 缁撴灉;
-- END;
-- $$ LANGUAGE plpgsql;

-- CREATE TRIGGER trigger_name
-- AFTER|BEFORE INSERT|UPDATE|DELETE
-- ON table_name
-- FOR EACH ROW
-- EXECUTE FUNCTION function_name();


/*

Back Up tables

meaning of zbak: z as the last letter of the alphabet, bak as backup

*/

-- 姣旇禌琛?
CREATE TABLE zbak_match
(
    LIKE match INCLUDING ALL
);

CREATE TABLE zbak_match_manager
(
    LIKE match_manager INCLUDING ALL
);
-- 姣旇禌(鍙嬭皧璧?)閭?璇风悆闃?
CREATE TABLE zbak_match_team_request
(
    LIKE match_team_request INCLUDING ALL
);

-- 姣旇禌-瑁佸垽
CREATE TABLE zbak_match_referee
(
    LIKE match_referee INCLUDING ALL
);

-- 姣旇禌閭?璇疯鍒?
CREATE TABLE zbak_match_referee_request
(
    LIKE match_referee_request INCLUDING ALL
);


-- 姣旇禌-鐞冨憳琛屼负锛堣繘鐞冦?佺孩鐗屻?侀粍鐗岋級
CREATE TABLE zbak_match_player_action
(
    LIKE match_player_action INCLUDING ALL
);

CREATE TABLE zbak_match_live
(
    LIKE match_live INCLUDING ALL
);

CREATE TABLE zbak_match_video
(
    LIKE match_video INCLUDING ALL
);

CREATE TABLE zbak_match_player
(
    LIKE match_player INCLUDING ALL
);

-- 璧涗簨琛?
CREATE TABLE zbak_event
(
    LIKE event INCLUDING ALL
);

-- 璧涗簨绠＄悊鑰呰〃
CREATE TABLE zbak_event_manager
(
    LIKE event_manager INCLUDING ALL
);

-- 璧涗簨-鐞冮槦
CREATE TABLE zbak_event_team
(
    LIKE event_team INCLUDING ALL
);
-- 灏忕粍
CREATE TABLE zbak_event_group
(
    LIKE event_group INCLUDING ALL
);

-- 灏忕粍-鐞冮槦
CREATE TABLE zbak_event_group_team
(
    LIKE event_group_team INCLUDING ALL
);

-- 璧涗簨閭?璇风悆闃?/鐞冮槦鐢宠鍔犲叆璧涗簨
CREATE TABLE zbak_event_team_request
(
    LIKE event_team_request INCLUDING ALL
);

-- 璧涗簨-瑁佸垽
CREATE TABLE zbak_event_referee
(
    LIKE event_referee INCLUDING ALL
);

-- 璧涗簨閭?璇疯鍒?
CREATE TABLE zbak_event_referee_request
(
    LIKE event_referee_request INCLUDING ALL
);

-- 璧涗簨-姣旇禌闃舵锛氬皬缁勮禌銆佹窐姹拌禌銆佹帓浣嶈禌绛?
CREATE TABLE zbak_event_stage
(
    LIKE event_stage INCLUDING ALL
);

-- 銆愯禌浜?-姣旇禌闃舵銆?-鏍囩锛氬stage=灏忕粍璧涳紝tag=A缁勩?丅缁勭瓑锛泂tage=娣樻卑璧涳紝tag=1/8鍐宠禌銆?1/4鍐宠禌绛?
CREATE TABLE zbak_event_stage_tag
(
    LIKE event_stage_tag INCLUDING ALL
);

-- 璧涗簨-姣旇禌
CREATE TABLE zbak_event_match
(
    LIKE event_match INCLUDING ALL
);

CREATE TABLE zbak_match_referee
(
    LIKE match_referee
);

CREATE TABLE zbak_match_live
(
    LIKE match_live
);

CREATE TABLE zbak_match_player
(
    LIKE match_player
);

CREATE TABLE zbak_match_video
(
    LIKE match_video
);

CREATE TABLE zbak_match_player_action
(
    LIKE match_player_action
);



CREATE
    OR REPLACE FUNCTION backup_and_delete_match()
    RETURNS TRIGGER AS
$$
BEGIN
    -- 澶嶅埗琛ˋ鐨勮鍒犻櫎琛屽埌澶囦唤琛?
    INSERT INTO zbak_match
    SELECT *
    FROM match
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_event_match
    SELECT *
    FROM event_match
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_manager
    SELECT *
    FROM match_manager
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_referee
    SELECT *
    FROM match_referee
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_player_action
    SELECT *
    FROM match_player_action
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_live
    SELECT *
    FROM match_live
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_video
    SELECT *
    FROM match_video
    WHERE match_id = OLD.match_id;

    INSERT INTO zbak_match_player
    SELECT *
    FROM match_player
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_player
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_video
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_live
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_player_action
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_referee_request
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_referee
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_team_request
    WHERE match_id = OLD.match_id;

    DELETE
    FROM match_manager
    WHERE match_id = OLD.match_id;

    DELETE
    FROM event_match
    WHERE match_id = OLD.match_id;

    DELETE
    FROM favorite_match
    WHERE match_id = OLD.match_id;

-- 杩斿洖OLD浠ュ厑璁稿垹闄ゆ搷浣滅户缁?
    RETURN OLD;
END
$$
    LANGUAGE plpgsql;

CREATE TRIGGER trigger_backup_and_delete
    BEFORE DELETE
    ON match
    FOR EACH ROW
EXECUTE FUNCTION backup_and_delete_match();

CREATE
    OR REPLACE FUNCTION backup_and_delete_event()
    RETURNS TRIGGER AS
$$
BEGIN

    -- 寮?濮嬪鍒?

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

-- 寮?濮嬪垹闄?

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
    FROM event_team
    WHERE event_id = OLD.event_id;

    DELETE
    FROM event_manager
    WHERE event_id = OLD.event_id;

    DELETE
    FROM favorite_event
    WHERE event_id = OLD.event_id;

-- 杩斿洖OLD浠ュ厑璁稿垹闄ゆ搷浣滅户缁?
    RETURN OLD;
END;
$$
    LANGUAGE plpgsql;

-- 鍒涘缓瑙﹀彂鍣?
CREATE TRIGGER trigger_backup_and_delete
    BEFORE DELETE
    ON event
    FOR EACH ROW
EXECUTE FUNCTION backup_and_delete_event();

-- 鏂囦欢hash琛?
CREATE TABLE file_hash
(
    file_id  SERIAL PRIMARY KEY,
    hash     VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL
);

-- -------------
-- 涓夌骇鏉冮檺鍒剁浉鍏崇殑琛?

-- 绗竴绾ф潈闄愯〃
CREATE TABLE first_level_authority
(
    username     VARCHAR(255) PRIMARY KEY ,
    password     VARCHAR(255) NOT NULL
);

-- 绗簩绾ф潈闄愯〃
CREATE TABLE second_level_authority
(
    authority_id   SERIAL PRIMARY KEY,
    username       VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    description    TEXT,
    create_user_id INT REFERENCES t_user
);

-- 绗笁绾ф潈闄愯〃
CREATE TABLE third_level_authority
(
    authority_id              SERIAL PRIMARY KEY,
    second_level_authority_id INT REFERENCES second_level_authority,
    user_id                   INT REFERENCES t_user,
    description               TEXT,
    create_user_id            INT REFERENCES t_user
);

-- 鍏崇郴琛細鐞冮槦-鍒涘缓鑰?
CREATE TABLE team_creator
(
    team_id                INT REFERENCES team PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 鍒涘缓鐨勭敤鎴?
    create_authority_level INT DEFAULT 0,         -- 0锛氭湭鐭ワ紱1锛氫竴绾ф潈闄愬垱寤猴紱2锛氫簩绾ф潈闄愬垱寤猴紱3锛氫笁绾ф潈闄愬垱寤?
    create_authority_id    INT DEFAULT 0          -- 鍒涘缓鑰呰嫢涓轰簩绾ф潈闄愭垨涓夌骇鏉冮檺锛屽垯璁板綍鍏禝D锛屽惁鍒欎负0
);

-- 鍏崇郴琛細鍙嬭皧璧?-鍒涘缓鑰咃紙璧涗簨姣旇禌涓嶅湪姝よ褰曪級
CREATE TABLE match_creator
(
    match_id               INT REFERENCES match PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 鍒涘缓鐨勭敤鎴?
    create_authority_level INT DEFAULT 0,         -- 0锛氭湭鐭ワ紱1锛氫竴绾ф潈闄愬垱寤猴紱2锛氫簩绾ф潈闄愬垱寤猴紱3锛氫笁绾ф潈闄愬垱寤?
    create_authority_id    INT DEFAULT 0          -- 鍒涘缓鑰呰嫢涓轰簩绾ф潈闄愭垨涓夌骇鏉冮檺锛屽垯璁板綍鍏禝D锛屽惁鍒欎负0
);

-- 鍏崇郴琛細璧涗簨-鍒涘缓鑰?
CREATE TABLE event_creator
(
    event_id               INT REFERENCES event PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 鍒涘缓鐨勭敤鎴?
    create_authority_level INT DEFAULT 0,         -- 0锛氭湭鐭ワ紱1锛氫竴绾ф潈闄愬垱寤猴紱2锛氫簩绾ф潈闄愬垱寤猴紱3锛氫笁绾ф潈闄愬垱寤?
    create_authority_id    INT DEFAULT 0          -- 鍒涘缓鑰呰嫢涓轰簩绾ф潈闄愭垨涓夌骇鏉冮檺锛屽垯璁板綍鍏禝D锛屽惁鍒欎负0
);