-- =====================================================
-- 审计日志表迁移
--
-- 背景：OperationLog 实体与 OperationLogMapper 接口一直存在，但
--   1) db_music 中没有 operation_log 表（information_schema 实测该表不存在）；
--   2) resources/mapper 下没有 OperationLogMapper.xml，接口 7 个方法全无绑定语句。
-- 也就是说审计链不是"没接线"这么简单，而是表与 SQL 都缺失——
-- 任何调用方一旦注入该 mapper 并调用，运行期即抛 BindingException。
-- 本迁移只负责把表建起来，XML 与接线在对应提交中完成。
-- =====================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`               INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          INT NULL COMMENT '操作用户ID，未登录操作可为空',
    `username`         VARCHAR(50) NULL COMMENT '操作用户名',
    `user_type`        VARCHAR(20) NULL COMMENT '用户类型(admin/consumer)',
    `operation_type`   VARCHAR(20) NULL COMMENT '操作类型(LOGIN/LOGOUT/ADD/UPDATE/DELETE/UPLOAD)',
    `module`           VARCHAR(50) NULL COMMENT '操作模块',
    `description`      VARCHAR(255) NULL COMMENT '操作描述',
    `request_url`      VARCHAR(255) NULL COMMENT '请求URL',
    `request_method`   VARCHAR(10) NULL COMMENT '请求方法',
    `request_params`   TEXT NULL COMMENT '请求参数（已脱敏，不含口令字段）',
    `response_result`  TEXT NULL COMMENT '响应结果摘要',
    `ip_address`       VARCHAR(50) NULL COMMENT '操作IP地址',
    `duration`         BIGINT NULL COMMENT '操作耗时(毫秒)',
    `status`           VARCHAR(20) NULL COMMENT '操作状态(SUCCESS/FAILED)',
    `error_msg`        VARCHAR(500) NULL COMMENT '错误信息',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_operation_log_create_time` (`create_time`) USING BTREE,
    KEY `idx_operation_log_user_id` (`user_id`) USING BTREE,
    KEY `idx_operation_log_type_time` (`operation_type`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';
