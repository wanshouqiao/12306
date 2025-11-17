-- 12306 数据库初始化脚本
-- 请先确保 MySQL 已启动，然后在 MySQL 客户端中执行此脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `12306` 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 切换到数据库
USE `12306`;

-- 显示提示信息
SELECT '数据库创建成功！' AS message;
SELECT '请继续导入数据表：mysql -u root -p 12306 < resources/db/12306-springboot.sql' AS next_step;

