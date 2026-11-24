-- 管理员账号：admin / cashfans03
-- 密码用 PBKDF2-HMAC-SHA256（120000 轮、16 字节随机盐）存进 app_user.password_hash，
-- 格式 pbkdf2$<迭代次数>$<盐 base64>$<哈希 base64>，与后端 PasswordHasher 对应。
-- 可重复执行：命中 uk_handle 时只更新密码与状态。
INSERT INTO app_user
  (handle, nickname, avatar_hue, city, role, bio, followers_count, following_count, verified, password_hash, status)
VALUES
  ('admin', '管理员', 122, '乌鲁木齐', '管理员', '站点管理员账号', 128, 12, 1,
   'pbkdf2$120000$WPVuFmiuUxldPZu3PB06oQ==$a/aQkRSXHXCBKcfHHnkJNa/bNwILMisMpASkxkXhSKE=', 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  status = VALUES(status);
