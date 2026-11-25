-- ============================================================
-- 增量迁移：注册后引导（喜欢的风格 / 艺人 / 身份 / 年龄 / 城市）
-- 目标库：Undercurrent_test
-- 说明：只加列 / 加表，可重复执行
-- ============================================================

USE Undercurrent_test;

-- 1) app_user.age_group：年龄段（引导第三步采集，非生日，避免强制实名信息）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'age_group'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE app_user ADD COLUMN age_group VARCHAR(20) NULL COMMENT ''年龄段：18 岁以下/18-24/25-30/31-40/40 岁以上'' AFTER role',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) app_user.onboarded_at：走过引导（含跳过）的时间，NULL 表示还没走过
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'Undercurrent_test' AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'onboarded_at'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE app_user ADD COLUMN onboarded_at DATETIME NULL COMMENT ''完成/跳过注册引导的时间，NULL=未走过'' AFTER updated_at',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 存量用户（含种子账号）标记为已走过，避免老账号登录被引导拦一次
UPDATE app_user SET onboarded_at = NOW() WHERE onboarded_at IS NULL;

-- 4) 用户偏好：一张表装流派与艺人，kind 区分；后续做「按兴趣推荐」直接用
CREATE TABLE IF NOT EXISTS user_interest (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  kind       VARCHAR(20)     NOT NULL COMMENT 'genre=流派 / artist=艺人',
  ref        VARCHAR(50)     NOT NULL COMMENT '流派名，或艺人 user id',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_kind_ref (user_id, kind, ref),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好（注册引导采集）';
