-- Schema for test database (SQLite in-memory)
CREATE TABLE IF NOT EXISTS test_entity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_time BIGINT DEFAULT 0
);

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key      TEXT    NOT NULL,
    config_value    TEXT    NOT NULL DEFAULT '',
    value_type      TEXT    NOT NULL DEFAULT 'STRING',
    group_code      TEXT    NOT NULL,
    display_name    TEXT    NOT NULL,
    description     TEXT    DEFAULT '',
    input_type      TEXT    NOT NULL DEFAULT 'TEXT',
    input_config    TEXT    DEFAULT '',
    sort            INTEGER NOT NULL DEFAULT 0,
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL,
    UNIQUE(config_key)
);

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL,
    password_hash   TEXT    NOT NULL,
    nickname        TEXT    NOT NULL DEFAULT '',
    status          TEXT    NOT NULL DEFAULT 'ACTIVE',
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL,
    UNIQUE(username)
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trace_id        TEXT    NOT NULL DEFAULT '',
    user_id         TEXT    NOT NULL DEFAULT '',
    module          TEXT    NOT NULL DEFAULT '',
    operation_type  TEXT    NOT NULL DEFAULT '',
    description     TEXT    NOT NULL DEFAULT '',
    method          TEXT    NOT NULL DEFAULT '',
    params          TEXT    DEFAULT '',
    result          TEXT    DEFAULT '',
    execution_time  INTEGER NOT NULL DEFAULT 0,
    ip              TEXT    NOT NULL DEFAULT '',
    status          TEXT    NOT NULL DEFAULT 'SUCCESS',
    error_message   TEXT    DEFAULT '',
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL
);
