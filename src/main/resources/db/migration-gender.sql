-- ============================================================
-- 增量迁移：注册引导增加性别字段
-- 目标库：Undercurrent_test
-- 说明：只加列，可重复执行
-- 执行前请先备份 app_user 表
-- ============================================================

USE Undercurrent_test;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'gender'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE app_user ADD COLUMN gender VARCHAR(10) NULL COMMENT ''性别/自我认同：男/女/其他/T/Le/沃尔玛塑料袋等'' AFTER age_group',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
