CREATE DATABASE IF NOT EXISTS testdb;

USE testdb;

-- ================================================================================
-- 表模板，AI禁止修改此模板！！！
-- ================================================================================
-- CREATE TABLE IF NOT EXISTS `TODO 表名`
-- (
--     -- 主键
--     `id`          BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
--     -- 审计字段
--     `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--     `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
--     `create_user` VARCHAR(64)        DEFAULT NULL COMMENT '创建人ID',
--     `update_user` VARCHAR(64)        DEFAULT NULL COMMENT '更新人ID',
--     -- 逻辑删除字段
--     `delete_time` BIGINT    NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，非0=删除时间戳',
--     `delete_user` VARCHAR(64)        DEFAULT NULL COMMENT '删除人ID',
--
--     -- 业务字段
--
--     -- 主键和索引
--     PRIMARY KEY (`id`),
--     UNIQUE KEY `uk_<业务唯一字段>_delete_time` (`<业务唯一字段>`, `delete_time`) COMMENT '时间范围',
--     -- 普通的Key索引
-- ) ENGINE = InnoDB
--   DEFAULT CHARSET = utf8mb4
--   COLLATE = utf8mb4_unicode_ci COMMENT ='TODO';
--
-- ================================================================================
-- 说明
-- ================================================================================
-- 1. 所有表使用 InnoDB 引擎，支持事务和行级锁
-- 2. 所有表使用 utf8mb4 字符集，支持完整的Unicode字符（包括emoji）
-- 3. 所有表使用 utf8mb4_unicode_ci 排序规则，支持不区分大小写的比较
-- 4. 所有表使用 CREATE TABLE IF NOT EXISTS 语法，避免重复创建错误
-- 5. 所有表包含6个审计字段：create_time, update_time, create_user, update_user
-- 6. 所有索引命名遵循以下规则：
--    - 唯一索引：uk_表名_字段名（多个字段用下划线分隔）
--    - 普通索引：idx_表名_字段名（多个字段用下划线分隔）
-- 7. 乐观锁 version 可加可不加，视具体业务而定
-- 8. 不使用物理外键约束，使用逻辑外键（通过注释说明关联关系）
-- 9. 字段命名采用下划线命名法（从Java驼峰命名转换）
-- 10. 主键使用 BIGINT AUTO_INCREMENT，自增ID
-- 11. TIMESTAMP字段默认值为CURRENT_TIMESTAMP，更新时间字段使用ON UPDATE CURRENT_TIMESTAMP自动更新
-- 12. 逻辑删除字段（delete_time, delete_user）用于软删除，NULL表示未删除
-- 13. JSON类型用于JSON序列化对象
-- 14. TEXT类型用于存储大字段（复杂文本、错误信息等）
-- 15. VARCHAR长度根据实际业务需求选择（32/64/128/256/512）
-- 16. 组合索引字段顺序优化：等值条件字段优先，范围条件字段其次，排序字段最后
-- ================================================================================

-- ================================================================================
-- system_config - 系统配置表
-- ================================================================================
CREATE TABLE IF NOT EXISTS `system_config`
(
    -- 主键
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    -- 审计字段
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user`  VARCHAR(64)           DEFAULT NULL COMMENT '创建人ID',
    `update_user`  VARCHAR(64)           DEFAULT NULL COMMENT '更新人ID',
    -- 逻辑删除字段
    `delete_time`  BIGINT       NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，非0=删除时间戳',
    `delete_user`  VARCHAR(64)           DEFAULT NULL COMMENT '删除人ID',

    -- 业务字段
    `config_key`   VARCHAR(128) NOT NULL COMMENT '配置键（全局唯一，如 site.name）',
    `config_value` TEXT         NOT NULL COMMENT '配置值（字符串存储，根据value_type解析）',
    `value_type`   VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING/INTEGER/DECIMAL/BOOLEAN/ENUM/ARRAY/JSON',
    `group_code`   VARCHAR(32)  NOT NULL COMMENT '分组代码：BASIC/EMAIL/STORAGE/SECURITY',
    `display_name` VARCHAR(128) NOT NULL COMMENT '显示名称（中文）',
    `description`  VARCHAR(512)          DEFAULT NULL COMMENT '配置说明',
    `input_type`   VARCHAR(32)  NOT NULL DEFAULT 'TEXT' COMMENT '输入控件类型：TEXT/TEXTAREA/NUMBER/SWITCH/SELECT/MULTI_SELECT/JSON_EDITOR',
    `input_config` JSON                  DEFAULT NULL COMMENT '输入控件配置（如选项列表、校验规则等）',
    `sort`         INT          NOT NULL DEFAULT 0 COMMENT '组内排序序号',

    -- 主键和索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`, `delete_time`),
    KEY `idx_group_code` (`group_code`),
    KEY `idx_sort` (`sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统配置表';

-- ================================================================================
-- 事件消费表
-- ================================================================================
CREATE TABLE IF NOT EXISTS `event`
(
    -- 主键
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    -- 审计字段
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人ID',
    `update_user`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人ID',
    -- 逻辑删除字段
    `delete_time`     BIGINT       NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，非0=删除时间戳',
    `delete_user`     VARCHAR(64)           DEFAULT NULL COMMENT '删除人ID',

    -- 业务字段
    `eid`             VARCHAR(64)  NOT NULL COMMENT '事件id',
    `action`          VARCHAR(256) NOT NULL COMMENT '事件动作 PUBLISH(发布)/CONSUME(消费)',
    `source`          VARCHAR(256) NOT NULL COMMENT '事件来源 INTERNAL(内部)/xxx(外部)',
    `type`            VARCHAR(256) NOT NULL COMMENT '事件类型',
    `status`          VARCHAR(32)  NOT NULL COMMENT '事件状态：PUBLISH CREATED(已创建)/READY(就绪)/PUBLISHED(已发布)；CONSUME READY(准备消费)/PROCESSING(处理中)/CONSUMED(已消费)/RETRY(重试中)/FAILED(失败)',
    `payload`         JSON                  DEFAULT NULL COMMENT '事件载荷（JSON格式）',
    `executor`        VARCHAR(256)          DEFAULT NULL COMMENT '执行者 发布者id或消费者id',
    `executor_group`  VARCHAR(256)          DEFAULT NULL COMMENT '执行者组 发布者组或消费者组',
    `message`         TEXT                  DEFAULT NULL COMMENT '事件消息',
    `trace_id`        VARCHAR(128)          DEFAULT NULL COMMENT '跟踪ID',

    -- 重试相关
    `retry_times`     INT                   DEFAULT 0 COMMENT '当前重试次数',
    `max_retry_times` INT                   DEFAULT 0 COMMENT '最大重试次数',
    `next_retry_time` TIMESTAMP    NULL     DEFAULT NULL COMMENT '下次重试时间',

    -- 主键和索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eid_action_executor` (`eid`, `action`, `executor_group`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='事件发布表';

-- ================================================================================
-- 文件元数据表
-- ================================================================================
CREATE TABLE IF NOT EXISTS `file_metadata`
(
    -- 主键
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    -- 审计字段
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user`  VARCHAR(64)           DEFAULT NULL COMMENT '创建人ID',
    `update_user`  VARCHAR(64)           DEFAULT NULL COMMENT '更新人ID',
    -- 逻辑删除字段
    `delete_time`  BIGINT       NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，非0=删除时间戳',
    `delete_user`  VARCHAR(64)           DEFAULT NULL COMMENT '删除人ID',

    -- 业务字段
    `md5`          VARCHAR(64)  NOT NULL COMMENT '文件MD5值',
    `content_type` VARCHAR(128) NOT NULL COMMENT '文件MIME类型',
    `size`         BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `url`          VARCHAR(512) NOT NULL COMMENT '文件访问URL',
    `url_expire`   TIMESTAMP    NULL     DEFAULT NULL COMMENT 'URL过期时间',
    `path`         VARCHAR(512) NOT NULL COMMENT '文件存储路径',

    -- 主键和索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_metadata_md5` (`md5`),
    KEY `idx_file_metadata_content_type` (`content_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件元数据表';

-- ================================================================================
-- 文件业务关联表
-- ================================================================================
CREATE TABLE IF NOT EXISTS `file_business`
(
    -- 主键
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- 业务字段
    `file_meta_id` VARCHAR(64)  NOT NULL COMMENT '文件ID，关联file_metadata.id（逻辑外键，无物理外键约束）',
    `business_id`  VARCHAR(64)  NOT NULL COMMENT '业务ID',
    `name`         VARCHAR(256) NOT NULL COMMENT '文件业务名称',
    `type`         VARCHAR(128) NOT NULL COMMENT '业务类型',
    `usage`        VARCHAR(128)          DEFAULT NULL COMMENT '使用场景',
    `sort`         INT                   DEFAULT 0 COMMENT '排序序号',
    `remark`       VARCHAR(512)          DEFAULT NULL COMMENT '备注',

    -- 审计字段
    `create_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user`  VARCHAR(64)           DEFAULT NULL COMMENT '创建人ID',
    `update_user`  VARCHAR(64)           DEFAULT NULL COMMENT '更新人ID',

    -- 逻辑删除字段
    `delete_time`  BIGINT       NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，非0=删除时间戳',
    `delete_user`  VARCHAR(64)           DEFAULT NULL COMMENT '删除人ID',

    -- 主键和索引
    PRIMARY KEY (`id`),
    KEY `idx_file_business_file_meta_id` (`file_meta_id`),
    KEY `idx_file_business_business_id` (`business_id`),
    KEY `idx_file_business_type` (`type`),
    KEY `idx_file_business_usage` (`usage`),
    KEY `idx_file_business_sort` (`sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件业务关联表';
