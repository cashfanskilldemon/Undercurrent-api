-- 2026-11-27：帖子相关列名 thread_id -> post_id，索引名同步
-- 幂等：用 information_schema 判断列是否存在，重复执行不报错
-- 目标库：Undercurrent_test（MySQL 5.7+）
-- 注意：执行前先备份；后端代码已用 post_id，脚本执行完再重启服务

SET @db := DATABASE();

-- post_tag
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_tag' AND COLUMN_NAME = 'thread_id');
SET @sql := IF(@exists > 0,
    'ALTER TABLE post_tag CHANGE COLUMN thread_id post_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- post_reply
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_reply' AND COLUMN_NAME = 'thread_id');
SET @sql := IF(@exists > 0,
    'ALTER TABLE post_reply CHANGE COLUMN thread_id post_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- post_like
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_like' AND COLUMN_NAME = 'thread_id');
SET @sql := IF(@exists > 0,
    'ALTER TABLE post_like CHANGE COLUMN thread_id post_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- post_save
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_save' AND COLUMN_NAME = 'thread_id');
SET @sql := IF(@exists > 0,
    'ALTER TABLE post_save CHANGE COLUMN thread_id post_id BIGINT UNSIGNED NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引名同步（MySQL 的 CHANGE COLUMN 不会改索引名，仅重命名，不影响功能）
SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_reply' AND INDEX_NAME = 'idx_thread_time');
SET @sql := IF(@exists > 0, 'ALTER TABLE post_reply RENAME INDEX idx_thread_time TO idx_post_time', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_like' AND INDEX_NAME = 'idx_thread');
SET @sql := IF(@exists > 0, 'ALTER TABLE post_like RENAME INDEX idx_thread TO idx_post', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'post_save' AND INDEX_NAME = 'idx_thread');
SET @sql := IF(@exists > 0, 'ALTER TABLE post_save RENAME INDEX idx_thread TO idx_post', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史通知 href 里的 /threads/ 前缀改成 /post/（前端也做了兼容映射，双保险）
UPDATE notification
   SET href = REPLACE(href, '/threads/', '/post/')
 WHERE href LIKE '%/threads/%';

-- 核对：4 张表都应只剩 post_id，无 thread_id
SELECT TABLE_NAME, COLUMN_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = @db
   AND TABLE_NAME IN ('post_tag','post_reply','post_like','post_save')
   AND COLUMN_NAME IN ('thread_id','post_id')
 ORDER BY TABLE_NAME, COLUMN_NAME;
