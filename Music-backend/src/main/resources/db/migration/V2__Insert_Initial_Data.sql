-- 插入初始数据
-- V2__Insert_Initial_Data.sql

-- 插入默认管理员账号(如果已存在则忽略)
-- 密码为 BCrypt 加密后的 '123456'
INSERT IGNORE INTO `admin` (`id`, `name`, `password`) VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');
