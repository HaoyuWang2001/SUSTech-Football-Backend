-- last_updated函数
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


-- 用户表
CREATE TABLE t_user
(
    user_id     SERIAL PRIMARY KEY,
    openid      VARCHAR(255) NOT NULL UNIQUE,
    session_key VARCHAR(255) NOT NULL,
    nick_name   VARCHAR(255),
    avatar_url  VARCHAR(255)
);

-- 球员表
CREATE TABLE player
(
    player_id      SERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    photo_url      VARCHAR(255),
    birth_date     DATE,
    height         INT,
    weight         INT,
    position       VARCHAR(100), -- 后卫、前锋等
    identity       VARCHAR(100), -- 本科生，研究生，教职工，其他
    shu_yuan       VARCHAR(100), -- 若本科生则有书院
    college        VARCHAR(255), -- 若本科生/研究生/教职工则有院系
    admission_year INT,          -- 入学年份
    bio            TEXT,         -- 个人简介
    user_id        INT UNIQUE REFERENCES t_user
);

-- 教练表
CREATE TABLE coach
(
    coach_id  SERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    photo_url VARCHAR(255),
    bio       TEXT,
    user_id   INT REFERENCES t_user
);

-- 裁判表
CREATE TABLE referee
(
    referee_id SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    photo_url  VARCHAR(255),
    bio        TEXT,
    user_id    INT REFERENCES t_user
);

-- 球队表
CREATE TABLE team
(
    team_id     SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    logo_url    VARCHAR(255),
    captain_id  INT REFERENCES player (player_id),
    description TEXT
);

-- 球队-队服
CREATE TABLE team_uniform
(
    team_id     INT REFERENCES team (team_id),
    uniform_url VARCHAR(255),
    PRIMARY KEY (team_id, uniform_url)
);

-- 球队管理者表
CREATE TABLE team_manager
(
    user_id  INT REFERENCES t_user,
    team_id  INT REFERENCES team,
    is_owner BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, team_id)
);

-- 球队-球员
CREATE TABLE team_player
(
    team_id   INT REFERENCES team (team_id),
    player_id INT REFERENCES player (player_id),
    number    INT DEFAULT 0, -- 球衣号码
    PRIMARY KEY (team_id, player_id)
);

-- 球队邀请球员/球员申请加入球队
CREATE TABLE team_player_request
(
    team_id      INT REFERENCES team (team_id),
    player_id    INT REFERENCES player (player_id),
    type         VARCHAR CHECK ( type IN ('INVITATION', 'APPLICATION') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (team_id, player_id, type)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON team_player_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- 球队-教练
CREATE TABLE team_coach
(
    team_id  INT REFERENCES team (team_id),
    coach_id INT REFERENCES coach (coach_id),
    PRIMARY KEY (team_id, coach_id)
);

-- 球队邀请教练
CREATE TABLE team_coach_request
(
    team_id      INT REFERENCES team (team_id),
    coach_id     INT REFERENCES coach (coach_id),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (team_id, coach_id)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON team_coach_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 比赛表
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

-- 比赛(友谊赛)邀请球队
CREATE TABLE match_team_request
(
    match_id     INT REFERENCES match,
    team_id      INT REFERENCES team,
    type         VARCHAR CHECK ( type IN ('HOME', 'AWAY') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (match_id, team_id)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON match_team_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 比赛-裁判
CREATE TABLE match_referee
(
    match_id   INT REFERENCES match,
    referee_id INT REFERENCES referee,
    PRIMARY KEY (match_id, referee_id)
);

-- 比赛邀请裁判
CREATE TABLE match_referee_request
(
    match_id     INT REFERENCES match,
    referee_id   INT REFERENCES referee,
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (match_id, referee_id)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON match_referee_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();

-- 比赛-球员行为（进球、红牌、黄牌）
CREATE TABLE match_player_action
(
    match_id  INT REFERENCES match,
    team_id   INT REFERENCES team,
    player_id INT REFERENCES player,
    action    VARCHAR CHECK ( action IN ('GOAL', 'ASSIST', 'YELLOW_CARD', 'RED_CARD', 'ON', 'OFF')
        ),
    time      INTEGER, -- 比赛开始的时间
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

-- 赛事表
CREATE TABLE event
(
    event_id           SERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    match_player_count INT, -- 比赛人数：5人制、7人制、8人制、11人制
    roster_size        INT  -- 大名单人数：每支参赛球队可报名的球员总数
);

-- 赛事管理者表
CREATE TABLE event_manager
(
    event_id INT REFERENCES event (event_id),
    user_id  INT REFERENCES t_user,
    is_owner BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (event_id, user_id)
);

-- 赛事-球队
CREATE TABLE event_team
(
    event_id INT REFERENCES event (event_id) ON DELETE CASCADE,
    team_id  INT REFERENCES team (team_id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, team_id)
);

-- 赛事-球队-大名单（存储每个球队在赛事中的参赛球员）
CREATE TABLE event_team_roster
(
    event_id  INT,
    team_id   INT,
    player_id INT REFERENCES player (player_id) ON DELETE CASCADE,
    number    INT, -- 球衣号码
    PRIMARY KEY (event_id, team_id, player_id),
    FOREIGN KEY (event_id, team_id) REFERENCES event_team (event_id, team_id) ON DELETE CASCADE
);

-- 小组
CREATE TABLE event_group
(
    group_id SERIAL PRIMARY KEY,
    event_id INT REFERENCES event (event_id),
    name     VARCHAR(255) NOT NULL
);

-- 小组-球队
CREATE TABLE event_group_team
(
    group_id          INT REFERENCES event_group (group_id),
    team_id           INT REFERENCES team (team_id),
    num_wins          INT DEFAULT 0, -- 胜场
    num_draws         INT DEFAULT 0, -- 平局
    num_losses        INT DEFAULT 0, -- 负场
    num_goals_for     INT DEFAULT 0, -- 进球数
    num_goals_against INT DEFAULT 0, -- 失球数
    score             INT DEFAULT 0, -- 积分
    PRIMARY KEY (group_id, team_id)
);

-- 赛事邀请球队/球队申请加入赛事
CREATE TABLE event_team_request
(
    event_id     INT REFERENCES event (event_id),
    team_id      INT REFERENCES team (team_id),
    type         VARCHAR CHECK ( type IN ('INVITATION', 'APPLICATION') ),
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (event_id, team_id, type)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON event_team_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 赛事-裁判
CREATE TABLE event_referee
(
    event_id   INT REFERENCES event,
    referee_id INT REFERENCES referee,
    PRIMARY KEY (event_id, referee_id)
);

-- 赛事邀请裁判
CREATE TABLE event_referee_request
(
    event_id     INT REFERENCES event,
    referee_id   INT REFERENCES referee,
    status       VARCHAR CHECK ( status IN ('PENDING', 'ACCEPTED', 'REJECTED') ) DEFAULT 'PENDING',
    last_updated TIMESTAMP,
    PRIMARY KEY (event_id, referee_id)
);

-- 更新last_updated触发器
CREATE TRIGGER update_last_updated_trigger
    BEFORE INSERT or
        UPDATE
    ON event_referee_request
    FOR EACH ROW
EXECUTE FUNCTION update_last_updated_column();


-- 赛事-比赛阶段：小组赛、淘汰赛、排位赛等
CREATE TABLE event_stage
(
    event_id INT REFERENCES event,
    stage    VARCHAR, -- 小组赛、淘汰赛、排位赛等
    PRIMARY KEY (event_id, stage)
);

-- 【赛事-比赛阶段】-标签：如stage=小组赛，tag=A组、B组等；stage=淘汰赛，tag=1/8决赛、1/4决赛等
CREATE TABLE event_stage_tag
(
    event_id INT REFERENCES event,
    stage    VARCHAR, -- 小组赛、淘汰赛、排位赛等
    tag      VARCHAR, -- A组、B组等；1/8决赛、1/4决赛等
    FOREIGN KEY (event_id, stage) REFERENCES event_stage,
    PRIMARY KEY (event_id, stage, tag)
);

-- 赛事-比赛
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

-- 通知表
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

-- 收藏用户表
CREATE TABLE favorite_user
(
    user_id     INT REFERENCES t_user (user_id),
    favorite_id INT REFERENCES t_user (user_id),
    PRIMARY KEY (user_id, favorite_id)
);

-- 收藏球队表
CREATE TABLE favorite_team
(
    user_id INT REFERENCES t_user (user_id),
    team_id INT REFERENCES team (team_id),
    PRIMARY KEY (user_id, team_id)
);

-- 收藏赛事表
CREATE TABLE favorite_event
(
    user_id  INT REFERENCES t_user (user_id),
    event_id INT REFERENCES event (event_id),
    PRIMARY KEY (user_id, event_id)
);

-- 收藏比赛表
CREATE TABLE favorite_match
(
    user_id  INT REFERENCES t_user (user_id),
    match_id INT REFERENCES match (match_id),
    PRIMARY KEY (user_id, match_id)
);

-- 比赛评论表
CREATE TABLE match_comment
(
    comment_id SERIAL PRIMARY KEY,
    user_id    INT REFERENCES t_user (user_id),
    match_id   INT REFERENCES match (match_id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    time       TIMESTAMP DEFAULT now()
);

-- 二级评论表
CREATE TABLE match_comment_reply
(
    reply_id   SERIAL PRIMARY KEY,
    user_id    INT REFERENCES t_user (user_id),
    comment_id INT REFERENCES match_comment (comment_id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    time       TIMESTAMP DEFAULT now()
);

-- 评论点赞表
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


-- 语法

-- CREATE FUNCTION function_name (参数列表)
-- RETURNS 返回类型 AS $$
-- DECLARE
--     -- 变量声明
-- BEGIN
--     -- 函数体，执行的SQL语句或逻辑
--     RETURN 结果;
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

-- 比赛表
CREATE TABLE zbak_match
(
    LIKE match INCLUDING ALL
);

CREATE TABLE zbak_match_manager
(
    LIKE match_manager INCLUDING ALL
);
-- 比赛(友谊赛)邀请球队
CREATE TABLE zbak_match_team_request
(
    LIKE match_team_request INCLUDING ALL
);

-- 比赛-裁判
CREATE TABLE zbak_match_referee
(
    LIKE match_referee INCLUDING ALL
);

-- 比赛邀请裁判
CREATE TABLE zbak_match_referee_request
(
    LIKE match_referee_request INCLUDING ALL
);


-- 比赛-球员行为（进球、红牌、黄牌）
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

-- 赛事表
CREATE TABLE zbak_event
(
    LIKE event INCLUDING ALL
);

-- 赛事管理者表
CREATE TABLE zbak_event_manager
(
    LIKE event_manager INCLUDING ALL
);

-- 赛事-球队
CREATE TABLE zbak_event_team
(
    LIKE event_team INCLUDING ALL
);

-- 赛事-球队-大名单
CREATE TABLE zbak_event_team_roster
(
    LIKE event_team_roster INCLUDING ALL
);

-- 小组
CREATE TABLE zbak_event_group
(
    LIKE event_group INCLUDING ALL
);

-- 小组-球队
CREATE TABLE zbak_event_group_team
(
    LIKE event_group_team INCLUDING ALL
);

-- 赛事邀请球队/球队申请加入赛事
CREATE TABLE zbak_event_team_request
(
    LIKE event_team_request INCLUDING ALL
);

-- 赛事-裁判
CREATE TABLE zbak_event_referee
(
    LIKE event_referee INCLUDING ALL
);

-- 赛事邀请裁判
CREATE TABLE zbak_event_referee_request
(
    LIKE event_referee_request INCLUDING ALL
);

-- 赛事-比赛阶段：小组赛、淘汰赛、排位赛等
CREATE TABLE zbak_event_stage
(
    LIKE event_stage INCLUDING ALL
);

-- 【赛事-比赛阶段】-标签：如stage=小组赛，tag=A组、B组等；stage=淘汰赛，tag=1/8决赛、1/4决赛等
CREATE TABLE zbak_event_stage_tag
(
    LIKE event_stage_tag INCLUDING ALL
);

-- 赛事-比赛
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
    -- 复制表A的被删除行到备份表
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

-- 返回OLD以允许删除操作继续
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

-- 创建触发器
CREATE TRIGGER trigger_backup_and_delete
    BEFORE DELETE
    ON event
    FOR EACH ROW
EXECUTE FUNCTION backup_and_delete_event();

-- 文件hash表
CREATE TABLE file_hash
(
    file_id  SERIAL PRIMARY KEY,
    hash     VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL
);

-- -------------
-- 三级权限制相关的表

-- 第一级权限表
CREATE TABLE first_level_authority
(
    username     VARCHAR(255) PRIMARY KEY ,
    password     VARCHAR(255) NOT NULL
);

-- 第二级权限表
CREATE TABLE second_level_authority
(
    authority_id   SERIAL PRIMARY KEY,
    username       VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    description    TEXT,
    create_user_id INT REFERENCES t_user
);

-- 第三级权限表
CREATE TABLE third_level_authority
(
    authority_id              SERIAL PRIMARY KEY,
    second_level_authority_id INT REFERENCES second_level_authority,
    user_id                   INT REFERENCES t_user,
    description               TEXT,
    create_user_id            INT REFERENCES t_user
);

-- 关系表：球队-创建者
CREATE TABLE team_creator
(
    team_id                INT REFERENCES team PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 创建的用户
    create_authority_level INT DEFAULT 0,         -- 0：未知；1：一级权限创建；2：二级权限创建；3：三级权限创建
    create_authority_id    INT DEFAULT 0          -- 创建者若为二级权限或三级权限，则记录其ID，否则为0
);

-- 关系表：友谊赛-创建者（赛事比赛不在此记录）
CREATE TABLE match_creator
(
    match_id               INT REFERENCES match PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 创建的用户
    create_authority_level INT DEFAULT 0,         -- 0：未知；1：一级权限创建；2：二级权限创建；3：三级权限创建
    create_authority_id    INT DEFAULT 0          -- 创建者若为二级权限或三级权限，则记录其ID，否则为0
);

-- 关系表：赛事-创建者
CREATE TABLE event_creator
(
    event_id               INT REFERENCES event PRIMARY KEY,
    user_id                INT REFERENCES t_user, -- 创建的用户
    create_authority_level INT DEFAULT 0,         -- 0：未知；1：一级权限创建；2：二级权限创建；3：三级权限创建
    create_authority_id    INT DEFAULT 0          -- 创建者若为二级权限或三级权限，则记录其ID，否则为0
);


-- 为request表增加has_read和is_deleted字段
alter table zbak_event_team_request
    add has_read bool default false not null,
    add is_deleted bool default false not null;
