-- ============================================================
-- 增量迁移：个人主页外部链接
-- 目标库：Undercurrent_test
-- 说明：只建表，可重复执行
-- ============================================================

USE Undercurrent_test;

CREATE TABLE IF NOT EXISTS user_link (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '所属用户 id',
  url         VARCHAR(500)    NOT NULL COMMENT '完整链接地址',
  title       VARCHAR(60)         NULL COMMENT '显示标题，为空时显示域名',
  sort_order  INT             NOT NULL DEFAULT 0 COMMENT '展示顺序',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_link_user (user_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人主页外部链接';
