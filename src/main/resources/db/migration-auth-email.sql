-- ============================================================
-- 增量迁移：支持邮箱注册（手机号改为「认证用」，不参与注册登录）
-- 目标库：Undercurrent_test
-- 说明：只加列 / 加唯一索引，可重复执行
-- ============================================================

USE Undercurrent_test;

-- 1) app_user.email
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'email'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE app_user ADD COLUMN email VARCHAR(120) NULL COMMENT ''邮箱，注册与找回密码用'' AFTER phone',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 邮箱唯一索引（多个 NULL 允许）
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND INDEX_NAME = 'uk_email'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE app_user ADD UNIQUE KEY uk_email (email)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
