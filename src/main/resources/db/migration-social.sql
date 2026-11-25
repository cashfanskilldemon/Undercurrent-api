-- 社交扩展：私信会话 / 私信消息 / 用户拉黑
-- 目标库：Undercurrent_test（MySQL 8，utf8mb4_unicode_ci）
-- 风格与 schema.sql 一致：无物理外键、联合主键去重、冗余计数在应用层维护
-- 可重复执行：全部 CREATE TABLE IF NOT EXISTS

-- 会话：两人一条，user_a < user_b 由应用层规范化后写入（防重复会话）
CREATE TABLE IF NOT EXISTS conversation (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_a          BIGINT UNSIGNED NOT NULL                COMMENT '参与者A（较小 id）',
  user_b          BIGINT UNSIGNED NOT NULL                COMMENT '参与者B（较大 id）',
  last_message_at DATETIME        NULL                    COMMENT '最后一条消息时间，用于会话列表排序',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pair (user_a, user_b),
  KEY idx_user_a (user_a, last_message_at),
  KEY idx_user_b (user_b, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话';

-- 私信消息：一条消息一行，is_read 表示接收方是否已读
CREATE TABLE IF NOT EXISTS direct_message (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  conversation_id BIGINT UNSIGNED NOT NULL                COMMENT '所属会话 id',
  sender_id       BIGINT UNSIGNED NOT NULL                COMMENT '发送者 id',
  content         VARCHAR(1000)   NOT NULL                COMMENT '消息正文',
  is_read         TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '0未读 1已读',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_conversation (conversation_id, id),
  KEY idx_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息';

-- 拉黑：联合主键天然防重复，拉黑后关注/私信需由应用层校验
CREATE TABLE IF NOT EXISTS user_block (
  blocker_id  BIGINT UNSIGNED NOT NULL COMMENT '拉黑者 id',
  blocked_id  BIGINT UNSIGNED NOT NULL COMMENT '被拉黑者 id',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (blocker_id, blocked_id),
  KEY idx_blocked (blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户拉黑关系';
