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

        User user = userMapper.selectAuthByHandle(key);
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

    /** 注册：账号即 handle，昵称留空则用账号，注册成功后可直接登录 */
    @Transactional
    public Long register(String account, String password, String nickname) {
        if (account == null || account.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入账号和密码");
        }
        String handle = account.trim().toLowerCase();
        if (userMapper.selectAuthByHandle(handle) != null) {
            throw new BusinessException(409, "该账号已被注册");
        }
        if (handle.equals(password.toLowerCase())) {
            throw new BusinessException(400, "密码不能和账号相同");
        }

        User user = new User();
        user.setHandle(handle);
        user.setNickname(nickname == null || nickname.isBlank() ? handle : nickname.trim());
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

    /**
     * 忘记密码：demo 环境没有邮件 / 短信服务，这里把申请写成一条站内通知发给所有管理员。
     * 无论账号是否存在都返回成功，避免被用来枚举账号。
     */
    @Transactional
    public void requestPasswordReset(String account) {
        if (account == null || account.isBlank()) {
            throw new BusinessException(400, "请输入账号");
        }
        User user = userMapper.selectAuthByHandle(account.trim());
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
