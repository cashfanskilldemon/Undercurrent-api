-- ============================================================
-- Undercurrent 后端数据库结构
-- 目标库：MySQL 5.7.36 / 8.0 均可用（utf8mb4 / utf8mb4_unicode_ci）
-- 注意：本脚本含 DROP TABLE，会清空重建，勿在生产库直接执行
-- 设计说明：不使用物理外键（互联网项目常规做法），仅建索引，
--           关联完整性由应用层保证，避免删除用户时级联阻塞。
-- ============================================================

CREATE DATABASE IF NOT EXISTS Undercurrent_test
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE Undercurrent_test;

-- ------------------------------------------------------------
-- 1. 用户（音乐人 / 制作人 / 插件作者 / 听众统一一张表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  handle          VARCHAR(50)     NOT NULL                COMMENT '唯一用户名，对应前端 @ghostwire',
  phone           VARCHAR(20)         NULL                COMMENT '手机号，手机号注册的账号才有',
  nickname        VARCHAR(50)     NOT NULL                COMMENT '昵称，对应前端 name',
  avatar_hue      SMALLINT UNSIGNED NOT NULL DEFAULT 0    COMMENT '头像色相 0-360，前端据此生成色块',
  city            VARCHAR(50)         NULL                COMMENT '城市',
  role            VARCHAR(20)     NOT NULL DEFAULT '听众'  COMMENT '音乐人/制作人/插件作者/听众',
  bio             VARCHAR(255)        NULL                COMMENT '简介',
  followers_count INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '粉丝数（冗余，便于排序）',
  following_count INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '关注数',
  verified        TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '认证徽章',
  password_hash   VARCHAR(100)        NULL                COMMENT '预留登录，BCrypt',
  status          TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_handle (handle),
  UNIQUE KEY uk_phone (phone),
  KEY idx_followers (followers_count DESC)
) ENGINE=InnoDB COMMENT='用户/音乐人';

-- ------------------------------------------------------------
-- 2. 字典表：流派 / 讨论频道 / 插件分类
-- ------------------------------------------------------------
DROP TABLE IF EXISTS genre;
CREATE TABLE genre (
  id         SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(30)       NOT NULL COMMENT 'Boom Bap / Trap / Phonk ...',
  sort_order SMALLINT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='音乐流派';

DROP TABLE IF EXISTS channel;
CREATE TABLE channel (
  id         SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(30)       NOT NULL COMMENT '作品反馈/混音母带/采样与版权/器材/现场演出/闲聊',
  slug       VARCHAR(30)       NOT NULL COMMENT 'URL 用，如 feedback',
  sort_order SMALLINT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_slug (slug)
) ENGINE=InnoDB COMMENT='讨论频道（「全部」为前端虚拟项，不入库）';

DROP TABLE IF EXISTS plugin_category;
CREATE TABLE plugin_category (
  id         SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(30)       NOT NULL COMMENT '合成器/效果器/鼓机/采样包/预设包',
  sort_order SMALLINT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='插件分类';

-- ------------------------------------------------------------
-- 3. 作品
-- ------------------------------------------------------------
DROP TABLE IF EXISTS track;
CREATE TABLE track (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  artist_id      BIGINT UNSIGNED NOT NULL,
  title          VARCHAR(120)    NOT NULL,
  genre_id       SMALLINT UNSIGNED    NULL,
  bpm            SMALLINT UNSIGNED    NULL,
  musical_key    VARCHAR(20)          NULL COMMENT '如 F 小调',
  duration_sec   INT UNSIGNED    NOT NULL DEFAULT 0,
  plays          INT UNSIGNED    NOT NULL DEFAULT 0,
  likes_count    INT UNSIGNED    NOT NULL DEFAULT 0,
  comments_count INT UNSIGNED    NOT NULL DEFAULT 0,
  cover          VARCHAR(120)         NULL COMMENT '封面文件名',
  blurb          VARCHAR(500)         NULL COMMENT '一句话描述',
  spec_json      JSON                 NULL COMMENT '鼓机序列/bass/chord，前端可视化用',
  released_at    DATETIME             NULL,
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_artist (artist_id),
  KEY idx_genre_release (genre_id, released_at DESC),
  KEY idx_plays (plays DESC)
) ENGINE=InnoDB COMMENT='作品';

DROP TABLE IF EXISTS track_tag;
CREATE TABLE track_tag (
  track_id BIGINT UNSIGNED NOT NULL,
  tag      VARCHAR(30)     NOT NULL,
  PRIMARY KEY (track_id, tag),
  KEY idx_tag (tag)
) ENGINE=InnoDB COMMENT='作品标签';

-- ------------------------------------------------------------
-- 4. 插件
-- ------------------------------------------------------------
DROP TABLE IF EXISTS plugin;
CREATE TABLE plugin (
  id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  maker_id      BIGINT UNSIGNED  NOT NULL COMMENT '作者，关联 app_user',
  name          VARCHAR(80)      NOT NULL,
  category_id   SMALLINT UNSIGNED     NULL,
  price         DECIMAL(10,2)    NOT NULL DEFAULT 0.00 COMMENT '0 表示免费',
  rating        DECIMAL(2,1)     NOT NULL DEFAULT 0.0,
  reviews_count INT UNSIGNED     NOT NULL DEFAULT 0,
  downloads     INT UNSIGNED     NOT NULL DEFAULT 0,
  formats       VARCHAR(60)           NULL COMMENT 'VST3,AU,CLAP 逗号分隔',
  os            VARCHAR(30)           NULL COMMENT 'Win,Mac 逗号分隔',
  version       VARCHAR(20)           NULL,
  file_size     VARCHAR(20)           NULL COMMENT '如 18.6 MB',
  hue           SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '封面色相',
  knobs         TINYINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '图形旋钮数量',
  tagline       VARCHAR(120)          NULL,
  description   TEXT                  NULL,
  updated_at    DATETIME              NULL,
  created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category_id),
  KEY idx_maker (maker_id),
  KEY idx_downloads (downloads DESC)
) ENGINE=InnoDB COMMENT='插件';

-- ------------------------------------------------------------
-- 5. 讨论帖 / 标签 / 回复
-- ------------------------------------------------------------
DROP TABLE IF EXISTS thread;
CREATE TABLE thread (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  author_id           BIGINT UNSIGNED NOT NULL,
  channel_id          SMALLINT UNSIGNED NOT NULL,
  body                TEXT            NOT NULL COMMENT '正文，可含 @提及',
  likes_count         INT UNSIGNED    NOT NULL DEFAULT 0,
  reposts_count       INT UNSIGNED    NOT NULL DEFAULT 0,
  replies_count       INT UNSIGNED    NOT NULL DEFAULT 0,
  pinned              TINYINT(1)      NOT NULL DEFAULT 0,
  attachment_kind     VARCHAR(20)          NULL COMMENT 'track / plugin，NULL 表示无附件',
  attachment_track_id BIGINT UNSIGNED      NULL,
  image               VARCHAR(120)         NULL COMMENT '发帖配图文件名，对应前端 /img/xxx',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_channel_time (channel_id, created_at DESC),
  KEY idx_author (author_id),
  KEY idx_pin_time (pinned DESC, created_at DESC)
) ENGINE=InnoDB COMMENT='讨论帖';

DROP TABLE IF EXISTS thread_tag;
CREATE TABLE thread_tag (
  thread_id BIGINT UNSIGNED NOT NULL,
  tag       VARCHAR(30)     NOT NULL,
  PRIMARY KEY (thread_id, tag),
  KEY idx_tag (tag)
) ENGINE=InnoDB COMMENT='帖子标签';

DROP TABLE IF EXISTS thread_reply;
CREATE TABLE thread_reply (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  thread_id   BIGINT UNSIGNED NOT NULL,
  author_id   BIGINT UNSIGNED NOT NULL,
  parent_id   BIGINT UNSIGNED      NULL COMMENT '楼中楼父回复',
  body        TEXT            NOT NULL,
  likes_count INT UNSIGNED    NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_thread_time (thread_id, created_at),
  KEY idx_parent (parent_id),
  KEY idx_author (author_id)
) ENGINE=InnoDB COMMENT='帖子回复';

-- ------------------------------------------------------------
-- 6. 互动关系（联合主键天然去重）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS thread_like;
CREATE TABLE thread_like (
  user_id    BIGINT UNSIGNED NOT NULL,
  thread_id  BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, thread_id),
  KEY idx_thread (thread_id)
) ENGINE=InnoDB COMMENT='帖子点赞';

DROP TABLE IF EXISTS thread_save;
CREATE TABLE thread_save (
  user_id    BIGINT UNSIGNED NOT NULL,
  thread_id  BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, thread_id),
  KEY idx_thread (thread_id)
) ENGINE=InnoDB COMMENT='帖子收藏';

DROP TABLE IF EXISTS track_like;
CREATE TABLE track_like (
  user_id    BIGINT UNSIGNED NOT NULL,
  track_id   BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, track_id),
  KEY idx_track (track_id)
) ENGINE=InnoDB COMMENT='作品点赞';

DROP TABLE IF EXISTS reply_like;
CREATE TABLE reply_like (
  user_id    BIGINT UNSIGNED NOT NULL,
  reply_id   BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, reply_id),
  KEY idx_reply (reply_id)
) ENGINE=InnoDB COMMENT='回复点赞';

DROP TABLE IF EXISTS user_follow;
CREATE TABLE user_follow (
  follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者',
  followee_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followee_id),
  KEY idx_followee (followee_id)
) ENGINE=InnoDB COMMENT='关注关系';

-- ------------------------------------------------------------
-- 7. 标签统计（右栏「推荐关注」的话题热度）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tag_stat;
CREATE TABLE tag_stat (
  tag   VARCHAR(30)     NOT NULL,
  posts INT UNSIGNED    NOT NULL DEFAULT 0,
  PRIMARY KEY (tag),
  KEY idx_posts (posts DESC)
) ENGINE=InnoDB COMMENT='标签帖子数';

-- ------------------------------------------------------------
-- 8. 通知（站点演示数据：like / reply / follow / system）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT UNSIGNED NOT NULL COMMENT '接收通知的用户',
  actor_id    BIGINT UNSIGNED      NULL COMMENT '触发者用户，system 类为 NULL',
  type        VARCHAR(20)     NOT NULL COMMENT 'like / reply / follow / system',
  body        VARCHAR(255)    NOT NULL COMMENT '通知文案',
  href        VARCHAR(120)         NULL COMMENT '点击跳转地址，如 /threads/1',
  is_read     TINYINT(1)      NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_recipient_time (recipient_id, is_read, created_at DESC)
) ENGINE=InnoDB COMMENT='站内通知';
