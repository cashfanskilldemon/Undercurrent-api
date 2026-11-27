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
  phone           VARCHAR(20)         NULL                COMMENT '手机号，留作实名认证用',
  email           VARCHAR(120)        NULL                COMMENT '邮箱，注册与找回密码用',
  nickname        VARCHAR(50)     NOT NULL                COMMENT '昵称，对应前端 name',
  avatar_hue      SMALLINT UNSIGNED NOT NULL DEFAULT 0    COMMENT '头像色相 0-360，前端据此生成色块',
  avatar          VARCHAR(200)     NULL                COMMENT '头像路径 /avatars/{id}.jpg，NULL 用色块',
  city            VARCHAR(50)         NULL                COMMENT '城市',
  role            VARCHAR(20)     NOT NULL DEFAULT '听众'  COMMENT '音乐人/制作人/插件作者/听众',
  age_group       VARCHAR(20)         NULL                COMMENT '年龄段：18 岁以下/18-24/25-30/31-40/40 岁以上（现也存准确年龄）',
  gender          VARCHAR(10)         NULL                COMMENT '性别/自我认同：男/女/其他/T/Le/沃尔玛塑料袋等',
  bio             VARCHAR(255)        NULL                COMMENT '简介',
  followers_count INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '粉丝数（冗余，便于排序）',
  following_count INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '关注数',
  verified        TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '认证徽章',
  password_hash   VARCHAR(100)        NULL                COMMENT '预留登录，BCrypt',
  status          TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  onboarded_at    DATETIME            NULL                COMMENT '完成/跳过注册引导的时间，NULL=未走过',
  PRIMARY KEY (id),
  UNIQUE KEY uk_handle (handle),
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_email (email),
  KEY idx_followers (followers_count DESC)
) ENGINE=InnoDB COMMENT='用户/音乐人';

-- ------------------------------------------------------------
-- 2. 字典表：流派 / 讨论频道 / 插件分类
-- ------------------------------------------------------------
DROP TABLE IF EXISTS genre;
CREATE TABLE genre (
  id         SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(30)       NOT NULL COMMENT 'Boom Bap / Trap / Phonk ...',
  category   VARCHAR(20)       NOT NULL DEFAULT '' COMMENT '分类：嘻哈/低音/电子/氛围/爵士/摇滚/民族/实验',
  sort_order SMALLINT          NOT NULL DEFAULT 0 COMMENT '分类序*100+类内序，读取时按它分组',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name),
  KEY idx_sort (sort_order)
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
DROP TABLE IF EXISTS post;
CREATE TABLE post (
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

DROP TABLE IF EXISTS post_tag;
CREATE TABLE post_tag (
  post_id BIGINT UNSIGNED NOT NULL,
  tag       VARCHAR(30)     NOT NULL,
  PRIMARY KEY (post_id, tag),
  KEY idx_tag (tag)
) ENGINE=InnoDB COMMENT='帖子标签';

DROP TABLE IF EXISTS post_reply;
CREATE TABLE post_reply (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id   BIGINT UNSIGNED NOT NULL,
  author_id   BIGINT UNSIGNED NOT NULL,
  parent_id   BIGINT UNSIGNED      NULL COMMENT '楼中楼父回复',
  body        TEXT            NOT NULL,
  likes_count INT UNSIGNED    NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_post_time (post_id, created_at),
  KEY idx_parent (parent_id),
  KEY idx_author (author_id)
) ENGINE=InnoDB COMMENT='帖子回复';

-- ------------------------------------------------------------
-- 6. 互动关系（联合主键天然去重）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS post_like;
CREATE TABLE post_like (
  user_id    BIGINT UNSIGNED NOT NULL,
  post_id  BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, post_id),
  KEY idx_post (post_id)
) ENGINE=InnoDB COMMENT='帖子点赞';

DROP TABLE IF EXISTS post_save;
CREATE TABLE post_save (
  user_id    BIGINT UNSIGNED NOT NULL,
  post_id  BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, post_id),
  KEY idx_post (post_id)
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
-- 改名字记录（14 天内最多 2 次）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_name_change (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL                COMMENT '用户 id',
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '改名时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, changed_at)
) ENGINE=InnoDB COMMENT='用户改名记录，用于 14 天两次限制';

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
  href        VARCHAR(120)         NULL COMMENT '点击跳转地址，如 /post/1',
  is_read     TINYINT(1)      NOT NULL DEFAULT 0,
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_recipient_time (recipient_id, is_read, created_at DESC)
) ENGINE=InnoDB COMMENT='站内通知';

-- ------------------------------------------------------------
-- 9. 私信会话（两人一条，user_a < user_b 由应用层规范化）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS conversation;
CREATE TABLE conversation (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_a          BIGINT UNSIGNED NOT NULL COMMENT '参与者A（较小 id）',
  user_b          BIGINT UNSIGNED NOT NULL COMMENT '参与者B（较大 id）',
  last_message_at DATETIME            NULL COMMENT '最后一条消息时间，会话列表排序',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pair (user_a, user_b),
  KEY idx_user_a (user_a, last_message_at),
  KEY idx_user_b (user_b, last_message_at)
) ENGINE=InnoDB COMMENT='私信会话';

-- ------------------------------------------------------------
-- 10. 私信消息
-- ------------------------------------------------------------
DROP TABLE IF EXISTS direct_message;
CREATE TABLE direct_message (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '所属会话 id',
  sender_id       BIGINT UNSIGNED NOT NULL COMMENT '发送者 id',
  content         VARCHAR(1000)   NOT NULL COMMENT '消息正文',
  is_read         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_conversation (conversation_id, id),
  KEY idx_sender (sender_id)
) ENGINE=InnoDB COMMENT='私信消息';

-- ------------------------------------------------------------
-- 11. 用户拉黑（联合主键天然防重复）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS user_block;
CREATE TABLE user_block (
  blocker_id  BIGINT UNSIGNED NOT NULL COMMENT '拉黑者 id',
  blocked_id  BIGINT UNSIGNED NOT NULL COMMENT '被拉黑者 id',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (blocker_id, blocked_id),
  KEY idx_blocked (blocked_id)
) ENGINE=InnoDB COMMENT='用户拉黑关系';

-- ------------------------------------------------------------
-- 12. 用户偏好（注册引导：喜欢的风格 / 艺人）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS user_interest;
CREATE TABLE user_interest (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  kind       VARCHAR(20)     NOT NULL COMMENT 'genre=流派 / artist=艺人（artist.id）',
  ref        VARCHAR(50)     NOT NULL COMMENT '流派名，或艺人 id（artist.id）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_kind_ref (user_id, kind, ref),
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='用户偏好（注册引导采集）';

-- ------------------------------------------------------------
-- 13. 艺人（网易云抓取的流派代表艺人，头像在 static/artists/）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS artist_genre;
DROP TABLE IF EXISTS artist;
CREATE TABLE artist (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  netease_id  BIGINT UNSIGNED NOT NULL COMMENT '网易云歌手 id（抓取来源，去重用）',
  name        VARCHAR(120)    NOT NULL COMMENT '艺名',
  alias       VARCHAR(200)    NULL     COMMENT '别名，逗号分隔',
  avatar      VARCHAR(255)    NULL     COMMENT '头像相对路径，如 /artists/2116.jpg',
  bio         VARCHAR(500)    NULL     COMMENT '一句话简介',
  music_size  INT             NOT NULL DEFAULT 0 COMMENT '网易云统计的歌曲数',
  album_size  INT             NOT NULL DEFAULT 0 COMMENT '网易云统计的专辑数',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_artist_netease (netease_id)
) ENGINE=InnoDB COMMENT='艺人（音乐人，与 app_user 解耦）';

CREATE TABLE artist_genre (
  artist_id  BIGINT UNSIGNED NOT NULL COMMENT '艺人 id',
  category   VARCHAR(40)     NOT NULL COMMENT '流派分类，如 嘻哈 / 说唱',
  sort_order INT             NOT NULL DEFAULT 0 COMMENT '该分类下的推荐顺序，从 1 开始',
  PRIMARY KEY (artist_id, category),
  KEY idx_artist_genre_cat (category, sort_order)
) ENGINE=InnoDB COMMENT='艺人所属流派';

-- ------------------------------------------------------------
-- 14. 个人主页链接（主页里的外部链接，一条 URL + 可选标题）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS user_link;
CREATE TABLE user_link (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '所属用户 id',
  url         VARCHAR(500)    NOT NULL COMMENT '完整链接地址',
  title       VARCHAR(60)         NULL COMMENT '显示标题，为空时显示域名',
  sort_order  INT             NOT NULL DEFAULT 0 COMMENT '展示顺序',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_link_user (user_id, sort_order)
) ENGINE=InnoDB COMMENT='个人主页外部链接';
