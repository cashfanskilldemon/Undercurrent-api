-- 2026-11-27：用户头像路径列
-- 幂等：information_schema 判断，重复执行不报错
-- 图片本体存外置 uploads/avatars/{user_id}.jpg，库里只存 /avatars/{id}.jpg?t=时间戳

SET @db := DATABASE();
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'avatar');
SET @sql := IF(@exists = 0,
    'ALTER TABLE app_user ADD COLUMN avatar VARCHAR(200) NULL AFTER avatar_hue',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COLUMN_NAME, COLUMN_TYPE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'avatar';
