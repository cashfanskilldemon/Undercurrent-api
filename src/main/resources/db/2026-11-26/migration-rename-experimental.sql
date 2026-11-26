-- ============================================================
-- 增量迁移：流派「实验 / 声音」改名「实验 / 前卫」
-- 目标库：Undercurrent_test
-- 说明：只改字符串，可重复执行
-- ============================================================

USE Undercurrent_test;

UPDATE genre SET category = '实验 / 前卫' WHERE category = '实验 / 声音';
UPDATE artist_genre SET category = '实验 / 前卫' WHERE category = '实验 / 声音';
