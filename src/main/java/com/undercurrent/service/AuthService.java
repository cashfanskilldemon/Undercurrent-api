package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.PasswordHasher;
import com.undercurrent.domain.User;
import com.undercurrent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 登录校验：账号即 app_user.handle，密码用 PBKDF2 哈希比对 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    /**
     * 校验账号密码，通过则返回用户 id。
     * 失败一律抛 401「账号或密码不正确」，不区分账号不存在 / 密码错，避免被枚举。
     */
    public Long authenticate(String account, String password) {
        if (account == null || account.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入账号和密码");
        }
        User user = userMapper.selectAuthByHandle(account.trim());
        if (user == null
                || user.getPasswordHash() == null
                || !PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "账号或密码不正确");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        // 旧格式（PBKDF2）或 BCrypt 代价因子偏低时，趁这次拿到明文密码顺手升级
        if (PasswordHasher.needsUpgrade(user.getPasswordHash())) {
            userMapper.updatePasswordHash(user.getId(), PasswordHasher.hash(password));
        }
        return user.getId();
    }
}
