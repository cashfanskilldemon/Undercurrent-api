package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.LoginAttemptService;
import com.undercurrent.common.PasswordHasher;
import com.undercurrent.domain.User;
import com.undercurrent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 登录校验：账号即 app_user.handle，密码用 BCrypt 比对，连续失败会锁定 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
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
}
