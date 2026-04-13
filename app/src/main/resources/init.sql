-- 基础配置 (BASIC)
INSERT OR IGNORE INTO system_config (config_key, config_value, value_type, group_code, display_name, description, input_type, input_config, sort) VALUES
('site.name', 'QuickStart Light', 'STRING', 'BASIC', '站点名称', '网站显示名称', 'TEXT', '', 1),
('site.description', 'A lightweight web application template', 'STRING', 'BASIC', '站点描述', '网站简短描述', 'TEXTAREA', '', 2),
('site.logo', '', 'STRING', 'BASIC', '站点Logo', '网站Logo URL', 'TEXT', '', 3),
('site.max_upload_size', '10', 'INTEGER', 'BASIC', '最大上传大小', '文件上传最大大小(MB)', 'NUMBER', '{"min":1,"max":100}', 4);

-- 邮件配置 (EMAIL)
INSERT OR IGNORE INTO system_config (config_key, config_value, value_type, group_code, display_name, description, input_type, input_config, sort) VALUES
('email.enabled', 'false', 'BOOLEAN', 'EMAIL', '启用邮件', '是否启用邮件发送功能', 'SWITCH', '', 1),
('email.from_address', 'noreply@example.com', 'STRING', 'EMAIL', '发件地址', '邮件发送者地址', 'TEXT', '', 2),
('email.from_alias', 'QuickStart', 'STRING', 'EMAIL', '发件别名', '邮件发送者显示名称', 'TEXT', '', 3),
('email.subject_prefix', '[QuickStart]', 'STRING', 'EMAIL', '主题前缀', '邮件主题前缀', 'TEXT', '', 4);

-- 存储配置 (STORAGE)
INSERT OR IGNORE INTO system_config (config_key, config_value, value_type, group_code, display_name, description, input_type, input_config, sort) VALUES
('storage.type', 'local', 'ENUM', 'STORAGE', '存储类型', '文件存储方式', 'SELECT', '{"options":["local","oss","minio"]}', 1),
('storage.local.path', './uploads', 'STRING', 'STORAGE', '本地存储路径', '本地文件存储根路径', 'TEXT', '', 2),
('storage.url_prefix', '/files', 'STRING', 'STORAGE', 'URL前缀', '文件访问URL前缀', 'TEXT', '', 3);

-- 安全配置 (SECURITY)
INSERT OR IGNORE INTO system_config (config_key, config_value, value_type, group_code, display_name, description, input_type, input_config, sort) VALUES
('security.login_max_attempts', '5', 'INTEGER', 'SECURITY', '最大登录尝试', '连续登录失败锁定前的最大尝试次数', 'NUMBER', '{"min":1,"max":20}', 1),
('security.lock_duration_minutes', '30', 'INTEGER', 'SECURITY', '锁定时长(分钟)', '账户锁定持续时间', 'NUMBER', '{"min":5,"max":1440}', 2),
('security.password_min_length', '8', 'INTEGER', 'SECURITY', '密码最小长度', '用户密码最小长度要求', 'NUMBER', '{"min":6,"max":32}', 3),
('security.enable_captcha', 'true', 'BOOLEAN', 'SECURITY', '启用验证码', '登录时是否需要验证码', 'SWITCH', '', 4);

-- 默认管理员账户（密码: admin123）
INSERT OR IGNORE INTO user (username, password_hash, nickname, status) VALUES
('admin', '$2a$10$b9P25YT02WoXL1HpGlcwUeD0E19TUIE6yq.V7QK5iMcWnSSw/.9.a', '系统管理员', 'ACTIVE');
