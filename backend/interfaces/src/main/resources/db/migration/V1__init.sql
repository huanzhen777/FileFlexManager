-- =====================================================
-- 表结构创建
-- =====================================================
CREATE TABLE IF NOT EXISTS task
(
    id                SERIAL PRIMARY KEY,
    type              VARCHAR(50) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    progress          INTEGER,
    message           TEXT,
    "desc"            TEXT,
    payload           TEXT,
    create_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    begin_time        TIMESTAMP,
    end_time          TIMESTAMP,
    scheduled         BOOLEAN              DEFAULT FALSE,
    from_schedule     BOOLEAN              DEFAULT FALSE,
    cron_expression   VARCHAR(100),
    enabled           BOOLEAN              DEFAULT FALSE,
    next_execute_time TIMESTAMP,
    execute_count     INTEGER              DEFAULT 0,
    last_execute_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users
(
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_index
(
    id            SERIAL PRIMARY KEY,
    path          TEXT      NOT NULL,
    name          TEXT      NOT NULL,
    size          BIGINT,
    mime_type     VARCHAR(100),
    md5           VARCHAR(32),
    is_dir        BOOLEAN            DEFAULT FALSE,
    parent_path   TEXT,
    create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP,
    hidden        BOOLEAN            DEFAULT FALSE,
    permissions   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS tag
(
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    parent_id    INTEGER,
    path         TEXT,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quick_access BOOLEAN               DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS file_tag
(
    id          SERIAL PRIMARY KEY,
    file_id     INTEGER   NOT NULL,
    tag_id      INTEGER   NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_config
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    "value"     TEXT,
    description VARCHAR(255),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 索引创建
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_task_status ON task (status);
CREATE INDEX IF NOT EXISTS idx_task_create_time ON task (create_time);
CREATE INDEX IF NOT EXISTS idx_task_scheduled ON task (scheduled);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

CREATE INDEX IF NOT EXISTS idx_file_index_path ON file_index (path);
CREATE INDEX IF NOT EXISTS idx_file_index_name ON file_index (name);
CREATE INDEX IF NOT EXISTS idx_file_index_parent_path ON file_index (parent_path);
CREATE INDEX IF NOT EXISTS idx_file_index_md5 ON file_index (md5);

CREATE INDEX IF NOT EXISTS idx_tag_name ON tag (name);
CREATE INDEX IF NOT EXISTS idx_tag_parent_id ON tag (parent_id);
CREATE INDEX IF NOT EXISTS idx_tag_path ON tag (path);

CREATE UNIQUE INDEX IF NOT EXISTS idx_file_tag_unique ON file_tag (file_id, tag_id);
CREATE INDEX IF NOT EXISTS idx_config_name ON t_config (name);