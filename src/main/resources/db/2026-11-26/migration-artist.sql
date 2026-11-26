-- ============================================================
-- 增量迁移：艺人库（网易云抓取的流派代表艺人 + 头像）
-- 目标库：Undercurrent_test
-- 说明：只加表，可重复执行
-- 数据：抓取脚本 scripts/fetch_netease_artists.py 生成 db/artist-seed.sql
-- ============================================================

USE Undercurrent_test;

-- 1) 艺人主表：一位艺人一行，netease_id 唯一，换分类不会重复建人
CREATE TABLE IF NOT EXISTS artist (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人（音乐人，与 app_user 解耦）';

-- 2) 艺人 × 流派：一位艺人可以落在多个流派，sort_order 是该流派里的推荐顺序
CREATE TABLE IF NOT EXISTS artist_genre (
  artist_id  BIGINT UNSIGNED NOT NULL COMMENT '艺人 id',
  category   VARCHAR(40)     NOT NULL COMMENT '流派分类，如 嘻哈 / 说唱',
  sort_order INT             NOT NULL DEFAULT 0 COMMENT '该分类下的推荐顺序，从 1 开始',
  PRIMARY KEY (artist_id, category),
  KEY idx_artist_genre_cat (category, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人所属流派';
