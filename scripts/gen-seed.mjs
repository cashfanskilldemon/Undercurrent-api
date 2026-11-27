import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
// mock 数据里键名仍是 threads（历史数据文件），生成的 SQL 已统一用 post
const data = JSON.parse(fs.readFileSync(path.join(ROOT, '../Undercurrent-web/data/mock-data.json'), 'utf8'));

const q = (s) => {
  if (s === null || s === undefined) return 'NULL';
  return "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
};
const num = (n) => (n === null || n === undefined ? 'NULL' : Number(n));

// 相对时间 → DATE_SUB(NOW(), INTERVAL n UNIT)
function relTime(str) {
  if (!str) return 'NOW()';
  const m = String(str).match(/(\d+)\s*(分钟|小时|天|周)前/);
  if (!m) return 'NOW()';
  const n = Number(m[1]);
  const unit = { 分钟: 'MINUTE', 小时: 'HOUR', 天: 'DAY', 周: 'WEEK' }[m[2]];
  return `DATE_SUB(NOW(), INTERVAL ${n} ${unit})`;
}

// "a1" → 1
const idOf = (code) => Number(String(code).replace(/^[a-z]+/i, ''));

const out = [];
out.push('-- 初始化数据：由 data/mock-data.json 生成');
out.push('USE Undercurrent_test;');
out.push('');
out.push('SET FOREIGN_KEY_CHECKS = 0;');
out.push('TRUNCATE TABLE post_like; TRUNCATE TABLE post_save; TRUNCATE TABLE track_like;');
out.push('TRUNCATE TABLE reply_like; TRUNCATE TABLE user_follow; TRUNCATE TABLE post_reply;');
out.push('TRUNCATE TABLE post_tag; TRUNCATE TABLE post; TRUNCATE TABLE track_tag;');
out.push('TRUNCATE TABLE track; TRUNCATE TABLE plugin; TRUNCATE TABLE app_user;');
out.push('TRUNCATE TABLE genre; TRUNCATE TABLE channel; TRUNCATE TABLE plugin_category;');
out.push('TRUNCATE TABLE tag_stat;');
out.push('SET FOREIGN_KEY_CHECKS = 1;');
out.push('');

// --- 字典 ---
out.push('-- 流派');
data.genres.forEach((g, i) => {
  out.push(`INSERT INTO genre (id, name, sort_order) VALUES (${i + 1}, ${q(g)}, ${i + 1});`);
});
out.push('');

out.push('-- 讨论频道（"全部"是前端虚拟项，不入库）');
const realChannels = data.channels.filter((c) => c !== '全部');
const channelSlug = {
  作品反馈: 'feedback', 混音母带: 'mixing', 采样与版权: 'sampling',
  器材: 'gear', 现场演出: 'live', 闲聊: 'chat',
};
realChannels.forEach((c, i) => {
  out.push(`INSERT INTO channel (id, name, slug, sort_order) VALUES (${i + 1}, ${q(c)}, ${q(channelSlug[c] || 'c' + (i + 1))}, ${i + 1});`);
});
out.push('');

out.push('-- 插件分类');
data.categories.forEach((c, i) => {
  out.push(`INSERT INTO plugin_category (id, name, sort_order) VALUES (${i + 1}, ${q(c)}, ${i + 1});`);
});
out.push('');

// --- 用户 ---
out.push('-- 用户（音乐人/制作人/插件作者/听众）');
data.artists.forEach((a, i) => {
  out.push(
    `INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (` +
    `${i + 1}, ${q(a.handle)}, ${q(a.name)}, ${num(a.hue)}, ${q(a.city)}, ${q(a.role)}, ${num(a.followers)}, ${a.verified ? 1 : 0}, NOW());`
  );
});
out.push('');

// --- 作品 ---
const genreId = new Map(data.genres.map((g, i) => [g, i + 1]));
out.push('-- 作品');
data.tracks.forEach((t, i) => {
  out.push(
    `INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (` +
    `${i + 1}, ${idOf(t.artistId)}, ${q(t.title)}, ${genreId.get(t.genre) || 'NULL'}, ${num(t.bpm)}, ${q(t.musicalKey)}, ${num(t.duration)}, ` +
    `${num(t.plays)}, ${num(t.likes)}, ${num(t.comments)}, ${q(t.cover)}, ${q(t.blurb)}, ${q(JSON.stringify(t.spec))}, ${relTime(t.released)});`
  );
});
out.push('');
out.push('-- 作品标签');
data.tracks.forEach((t, i) => {
  (t.tags || []).forEach((tag) => out.push(`INSERT INTO track_tag (track_id, tag) VALUES (${i + 1}, ${q(tag)});`));
});
out.push('');

// --- 插件 ---
const catId = new Map(data.categories.map((c, i) => [c, i + 1]));
out.push('-- 插件');
data.plugins.forEach((p, i) => {
  out.push(
    `INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (` +
    `${i + 1}, ${idOf(p.makerId)}, ${q(p.name)}, ${catId.get(p.category) || 'NULL'}, ${num(p.price)}, ${num(p.rating)}, ${num(p.reviews)}, ${num(p.downloads)}, ` +
    `${q((p.formats || []).join(','))}, ${q((p.os || []).join(','))}, ${q(p.version)}, ${q(p.size)}, ${num(p.hue)}, ${num(p.knobs)}, ` +
    `${q(p.tagline)}, ${q(p.description)}, ${relTime(p.updated)});`
  );
});
out.push('');

// --- 帖子 ---
const chanId = new Map(realChannels.map((c, i) => [c, i + 1]));
out.push('-- 帖子');
data.threads.forEach((th, i) => {
  const attachTrack = th.attachment && th.attachment.trackId ? idOf(th.attachment.trackId) : null;
  const attachKind = th.attachment ? th.attachment.kind : null;
  out.push(
    `INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (` +
    `${i + 1}, ${idOf(th.authorId)}, ${chanId.get(th.channel) || 1}, ${q(th.body)}, ${num(th.likes)}, ${num(th.reposts)}, ${(th.replies || []).length}, ` +
    `${th.pinned ? 1 : 0}, ${q(attachKind)}, ${attachTrack ?? 'NULL'}, ${relTime(th.time)});`
  );
});
out.push('');
out.push('-- 帖子标签');
data.threads.forEach((th, i) => {
  (th.tags || []).forEach((tag) => out.push(`INSERT INTO post_tag (post_id, tag) VALUES (${i + 1}, ${q(tag)});`));
});
out.push('');

// --- 回复 ---
out.push('-- 回复');
let replyId = 0;
data.threads.forEach((th, ti) => {
  (th.replies || []).forEach((r) => {
    replyId++;
    out.push(
      `INSERT INTO post_reply (id, post_id, author_id, body, likes_count, created_at) VALUES (` +
      `${replyId}, ${ti + 1}, ${idOf(r.authorId)}, ${q(r.body)}, ${num(r.likes)}, ${relTime(r.time)});`
    );
  });
});
out.push('');

// --- 标签统计 ---
out.push('-- 标签热度');
data.tags.forEach((t) => {
  out.push(`INSERT INTO tag_stat (tag, posts) VALUES (${q(t.tag)}, ${num(t.posts)});`);
});
out.push('');

fs.writeFileSync(path.join(ROOT, 'src/main/resources/db/seed.sql'), out.join('\n'), 'utf8');
console.log('seed.sql 已生成，行数:', out.length);
console.log('统计: 用户', data.artists.length, '| 作品', data.tracks.length, '| 插件', data.plugins.length,
  '| 帖子', data.threads.length, '| 回复', replyId, '| 标签', data.tags.length);
