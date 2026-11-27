-- 名字修改历史：14 天内最多改 2 次（参照 Threads / Instagram）
-- 幂等：表已存在则跳过
CREATE TABLE IF NOT EXISTS user_name_change (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id   BIGINT UNSIGNED NOT NULL                COMMENT '用户 id',
  changed_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '改名时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, changed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户改名字记录，用于 14 天两次限制';
