CREATE TABLE app_config (
    config_key   TEXT PRIMARY KEY,
    config_value TEXT,
    updated_at   TEXT NOT NULL DEFAULT (datetime('now'))
);

INSERT INTO app_config (config_key, config_value) VALUES ('schema_version', '1');
INSERT INTO app_config (config_key, config_value) VALUES ('app_name', 'SGFA');
