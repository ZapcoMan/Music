-- 修复 Flyway 失败的迁移记录
-- 执行此脚本前请确保数据库连接正常

USE db_music;

-- 查看当前的迁移历史
SELECT * FROM flyway_schema_history;

-- 删除版本2的失败记录
DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0;

-- 验证删除结果
SELECT * FROM flyway_schema_history;
