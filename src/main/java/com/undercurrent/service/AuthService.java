package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.LoginAttemptService;
import com.undercurrent.common.PasswordHasher;
import com.undercurrent.domain.User;
import com.undercurrent.mapper.NotificationMapper;
import com.undercurrent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 账号体系：登录、注册、忘记密码申请 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final LoginAttemptService loginAttemptService;

    /**
     * 校验账号密码，通过则返回用户 id。
     * 失败一律抛 401「账号或密码不正确」，不区分账号不存在 / 密码错，避免被枚举；
     * 同一账号连续失败达到阈值后抛 429 并锁定一段时间。
     */
    public Long authenticate(String account, String password) {
        if (account == null || account.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入账号和密码");
        }
        String key = account.trim();
        // 已锁定直接拒绝，连密码都不校验
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

        // 旧格式（PBKDF2）或 BCrypt 代价因子偏低时，趁这次拿到明文密码顺手升级
        if (PasswordHasher.needsUpgrade(user.getPasswordHash())) {
            userMapper.updatePasswordHash(user.getId(), PasswordHasher.hash(password));
        }
        return user.getId();
    }

    /** 注册：支持「账号」或「手机号」两种方式，成功后可直接登录 */
    @Transactional
    public Long register(String account, String phone, String password, String nickname) {
        if (password == null || password.length() < 6) {
            throw new BusinessException(400, "密码至少 6 位");
        }

        boolean byPhone = phone != null && !phone.isBlank();
        String handle;
        String normalizedPhone = null;

        if (byPhone) {
            normalizedPhone = phone.trim();
            if (!normalizedPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(400, "请输入正确的手机号");
            }
            if (userMapper.selectAuthByAccount(normalizedPhone) != null) {
                throw new BusinessException(409, "该手机号已注册");
            }
            handle = generateHandle(normalizedPhone);
        } else {
            if (account == null || account.isBlank()) {
                throw new BusinessException(400, "请输入手机号或账号");
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
        user.setPhone(normalizedPhone);
        user.setNickname(resolveNickname(nickname, normalizedPhone, handle));
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

    /** 昵称：没填就按手机号后四位生成「乐迷1234」，账号注册则直接用账号 */
    private String resolveNickname(String nickname, String phone, String handle) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        if (phone != null) {
            return "乐迷" + phone.substring(phone.length() - 4);
        }
        return handle;
    }

    /** 手机号注册时自动生成 handle（如 u1234567），保证唯一，避免 @13800138000 这种难看的用户名 */
    private String generateHandle(String phone) {
        String tail = phone.substring(phone.length() - 4);
        for (int i = 0; i < 8; i++) {
            String candidate = "u" + tail + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (userMapper.selectAuthByAccount(candidate) == null) {
                return candidate;
            }
        }
        throw new BusinessException(500, "注册繁忙，请稍后重试");
    }

    /**
     * 忘记密码：demo 环境没有邮件 / 短信服务，这里把申请写成一条站内通知发给所有管理员。
     * 无论账号是否存在都返回成功，避免被用来枚举账号。
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
