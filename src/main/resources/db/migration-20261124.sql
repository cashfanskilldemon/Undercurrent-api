-- ============================================================
-- 增量迁移 2026-11-24：为「首页/发现/通知/发帖/作品上传」补表与字段
-- 目标库：Undercurrent_test
-- 说明：只做 ALTER / CREATE IF NOT EXISTS，不 DROP、不清空现有数据
-- ============================================================

USE Undercurrent_test;

-- 1) 帖子配图字段（MySQL 5.7/8 均支持；列已存在则跳过）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'thread' AND COLUMN_NAME = 'image'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE thread ADD COLUMN image VARCHAR(120) NULL COMMENT ''发帖配图文件名'' AFTER attachment_track_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 通知表
CREATE TABLE IF NOT EXISTS notification (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT UNSIGNED NOT NULL COMMENT '接收通知的用户',
  actor_id     BIGINT UNSIGNED      NULL COMMENT '触发者用户，system 类为 NULL',
  type         VARCHAR(20)     NOT NULL COMMENT 'like / reply / follow / system',
  body         VARCHAR(255)    NOT NULL COMMENT '通知文案',
  href         VARCHAR(120)         NULL COMMENT '点击跳转地址',
  is_read      TINYINT(1)      NOT NULL DEFAULT 0,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_recipient_time (recipient_id, is_read, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知';

-- 3) 通知假数据：以用户 2（潮汐线 / tideline）为接收者，对齐 Web 版 6 条
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, 5, 'like',    '低压槽 赞了你在「地铁末班」下的回复', '/threads/1', 0, DATE_SUB(NOW(), INTERVAL 12 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'like' AND body LIKE '%地铁末班%');
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, 8, 'reply',   '磁带回收站 回复了你：采样清版权这事，早点做完比事后补救便宜', '/threads/2', 0, DATE_SUB(NOW(), INTERVAL 48 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'reply' AND body LIKE '%磁带回收站%');
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, 3, 'follow',  '灰烬 关注了你', '/u/ashe.mp3', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'follow');
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, NULL, 'system','本周地下榜更新：8 首新作品进入榜单', '/tracks', 0, DATE_SUB(NOW(), INTERVAL 5 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'system' AND body LIKE '%地下榜%');
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, 1, 'like',    '阿鬼 赞了你的帖子', '/threads', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'like' AND body LIKE '%阿鬼%');
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at)
SELECT 2, NULL, 'system','Soot 1.4.2 已发布，插件区可直接下载', '/plugins', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE recipient_id = 2 AND type = 'system' AND body LIKE '%Soot%');

-- 4) 关注关系假数据：让用户 2 关注若干音乐人，首页「关注流」才有内容
INSERT IGNORE INTO user_follow (follower_id, followee_id) VALUES
  (2, 1), (2, 3), (2, 5), (2, 9);

-- 5) 冗余「关注数」同步（直接重算，可重复执行）
-- 粉丝数 followers_count 是演示用的热度数字，不做覆写，避免误伤原有值
UPDATE app_user SET following_count = (
  SELECT COUNT(*) FROM user_follow WHERE follower_id = 2
) WHERE id = 2;
