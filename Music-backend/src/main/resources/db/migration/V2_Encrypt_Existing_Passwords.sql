-- V3__Encrypt_Existing_Passwords.sql
-- 将现有的明文密码转换为BCrypt加密密码

-- 注意：此脚本需要在应用启动前手动执行，或者在首次启动时执行
-- BCrypt加密后的密码示例：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy (对应密码: 123456)

-- 更新管理员密码为BCrypt加密后的 '123456'
UPDATE `admin` SET `password` = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE `name` = 'admin';

-- 如果需要更新其他已有用户的密码，可以类似处理
-- UPDATE `consumer` SET `password` = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE `username` = 'xxx';
