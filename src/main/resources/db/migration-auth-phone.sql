-- ============================================================
-- 增量迁移：支持手机号注册
-- 目标库：Undercurrent_test
-- 说明：只加列 / 加唯一索引，不删不改现有数据，可重复执行
-- ============================================================

USE Undercurrent_test;

-- 1) app_user.phone（可空，历史账号没有手机号）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'phone'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE app_user ADD COLUMN phone VARCHAR(20) NULL COMMENT ''手机号，手机号注册的账号才有'' AFTER handle',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 手机号唯一索引（多个 NULL 允许，不影响老数据）
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND INDEX_NAME = 'uk_phone'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE app_user ADD UNIQUE KEY uk_phone (phone)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
