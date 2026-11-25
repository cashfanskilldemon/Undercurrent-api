package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.LoginAttemptService;
import com.undercurrent.common.PasswordHasher;
import com.undercurrent.common.VerificationCodeService;
import com.undercurrent.domain.User;
import com.undercurrent.mapper.NotificationMapper;
import com.undercurrent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 账号体系：注册（邮箱 / 账号）、登录、邮箱验证码重置密码。
 * 手机号注册已下线，phone 字段留作后续实名认证使用。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String EMAIL_RE = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";

    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final LoginAttemptService loginAttemptService;
    private final VerificationCodeService verificationCodeService;

    /**
     * 校验账号密码，通过则返回用户 id。account 可以是 handle、邮箱或手机号。
     * 失败一律抛 401，不区分账号不存在 / 密码错，避免被枚举；连续失败会锁定。
     */
    public Long authenticate(String account, String password) {
        if (account == null || account.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入账号和密码");
        }
        String key = account.trim();
        loginAttemptService.assertNotLocked(key);

        User user = userMapper.selectAuthByAccount(key);
        boolean matched = user != null
                && user.getPasswordHash() != null
                && PasswordHasher.matches(password, user.getPasswordHash());

        if (!matched) {
            if (loginAttemptService.recordFailure(key)) {
                throw new BusinessException(429, "密码错误次数过多，账号已临时锁定，请稍后再试");
            }
            throw new BusinessException(401, "账号或密码不正确");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        loginAttemptService.reset(key);

        if (PasswordHasher.needsUpgrade(user.getPasswordHash())) {
            userMapper.updatePasswordHash(user.getId(), PasswordHasher.hash(password));
        }
        return user.getId();
    }

    /** 注册：邮箱（需验证码）或账号两种方式 */
    @Transactional
    public Long register(String account, String email, String password, String nickname, String emailCode) {
        if (password == null || password.length() < 6) {
            throw new BusinessException(400, "密码至少 6 位");
        }

        boolean byEmail = email != null && !email.isBlank();
        String handle;
        String normalizedEmail = null;

        if (byEmail) {
            normalizedEmail = email.trim().toLowerCase();
            if (!normalizedEmail.matches(EMAIL_RE)) {
                throw new BusinessException(400, "请输入正确的邮箱");
            }
            if (userMapper.selectAuthByAccount(normalizedEmail) != null) {
                throw new BusinessException(409, "该邮箱已被注册");
            }
            verificationCodeService.verify(normalizedEmail, VerificationCodeService.SCENE_REGISTER, emailCode);
            handle = generateHandle(normalizedEmail.substring(0, normalizedEmail.indexOf('@')));
        } else {
            if (account == null || account.isBlank()) {
                throw new BusinessException(400, "请输入邮箱或账号");
            }
            handle = account.trim().toLowerCase();
            if (userMapper.selectAuthByAccount(handle) != null) {
                throw new BusinessException(409, "该账号已被注册");
            }
            if (handle.equals(password.toLowerCase())) {
                throw new BusinessException(400, "密码不能和账号相同");
            }
        }

        User user = new User();
        user.setHandle(handle);
        user.setEmail(normalizedEmail);
        user.setNickname(resolveNickname(nickname, normalizedEmail, handle));
        user.setAvatarHue(ThreadLocalRandom.current().nextInt(0, 360));
        user.setRole("听众");
        user.setPasswordHash(PasswordHasher.hash(password));
        userMapper.insertUser(user);

        Long id = user.getId();
        if (id == null) {
            throw new BusinessException(500, "注册失败，请重试");
        }
        return id;
    }

    /** 昵称：没填就用邮箱前缀（截断到 12 字），账号注册则直接用账号 */
    private String resolveNickname(String nickname, String email, String handle) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        if (email != null) {
            String prefix = email.substring(0, email.indexOf('@'));
            return prefix.length() > 12 ? prefix.substring(0, 12) : prefix;
        }
        return handle;
    }

    /** 由邮箱前缀生成唯一 handle，如 alice + 3 位随机 */
    private String generateHandle(String prefix) {
        String base = prefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (base.length() < 3) {
            base = "user" + base;
        }
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        for (int i = 0; i < 8; i++) {
            String candidate = base + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (userMapper.selectAuthByAccount(candidate) == null) {
                return candidate;
            }
        }
        throw new BusinessException(500, "注册繁忙，请稍后重试");
    }

    /** 邮箱 + 验证码重置密码；成功后清掉登录失败计数 */
    @Transactional
    public void resetPassword(String email, String emailCode, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "密码至少 6 位");
        }
        String normalized = email.trim().toLowerCase();
        User user = userMapper.selectAuthByAccount(normalized);
        if (user == null) {
            throw new BusinessException(404, "该邮箱未注册");
        }
        verificationCodeService.verify(normalized, VerificationCodeService.SCENE_RESET, emailCode);

        userMapper.updatePasswordHash(user.getId(), PasswordHasher.hash(newPassword));
        loginAttemptService.reset(user.getHandle());
    }

    /** 发送验证码（注册 / 重置密码），返回明文码供 dev 环境回显 */
    public String sendCode(String target, String scene, VerificationCodeService.Channel channel) {
        return verificationCodeService.send(target.trim(), scene, channel);
    }

    /**
     * 旧版「忘记密码申请」：给管理员发站内通知（保留接口，前端已改用验证码重置）。
     */
    @Transactional
    public void requestPasswordReset(String account) {
        if (account == null || account.isBlank()) {
            throw new BusinessException(400, "请输入账号");
        }
        User user = userMapper.selectAuthByAccount(account.trim());
        if (user == null) {
            return;
        }
        List<Long> adminIds = userMapper.selectAdminIds();
        if (adminIds.isEmpty()) {
            return;
        }
        String body = "用户 @" + user.getHandle() + " 申请重置密码，请人工处理";
        for (Long adminId : adminIds) {
            notificationMapper.insert(adminId, user.getId(), "system", body, null);
        }
    }
}
