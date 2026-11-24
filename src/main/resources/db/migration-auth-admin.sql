-- 管理员账号：admin / cashfans03
-- 密码用 BCrypt（cost 12）存进 app_user.password_hash，字符串形如 $2a$12$...，
-- 与后端 PasswordHasher（spring-security-crypto）对应；旧的 pbkdf2 格式仍可校验，登录时自动升级。
-- 可重复执行：命中 uk_handle 时只更新密码与状态。
INSERT INTO app_user
  (handle, nickname, avatar_hue, city, role, bio, followers_count, following_count, verified, password_hash, status)
VALUES
  ('admin', '管理员', 122, '乌鲁木齐', '管理员', '站点管理员账号', 128, 12, 1,
   '$2a$12$3PIju6pvHQ2WVQA4Jh8YnexnyQsYw0HDh.AvKbcDVt8.50SGh76su', 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  status = VALUES(status);
