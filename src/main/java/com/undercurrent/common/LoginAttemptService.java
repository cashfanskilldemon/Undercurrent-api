package com.undercurrent.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流：同一账号连续失败 N 次后锁定 M 分钟（默认 5 次 / 10 分钟）。
 * 纯内存实现，单机够用；多实例部署时把计数换成 Redis 即可，对外方法不用变。
 * <p>按账号字符串计数（不管账号是否存在都计数），避免通过响应差异枚举用户。
 */
@Component
public class LoginAttemptService {

    private static final class State {
        /** 当前窗口内失败次数 */
        int failures;
        /** 计数窗口起点，0 表示尚未开始 */
        long windowStart;
        /** 锁定截止时间，0 表示未锁定 */
        long lockedUntil;
    }

    private final int maxFailures;
    private final long lockMillis;
    /** 超过这个时长没有新的失败，就重新开始计数，避免把很久以前的一次手滑累计进来 */
    private final long windowMillis;

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.auth.login-max-attempts:5}") int maxFailures,
            @Value("${app.auth.login-lock-minutes:10}") int lockMinutes,
            @Value("${app.auth.login-fail-window-minutes:30}") int windowMinutes) {
        this.maxFailures = Math.max(1, maxFailures);
        this.lockMillis = Duration.ofMinutes(Math.max(1, lockMinutes)).toMillis();
        this.windowMillis = Duration.ofMinutes(Math.max(1, windowMinutes)).toMillis();
    }

    /** 已锁定则抛 429；顺带清理过期记录，避免 map 无限增长 */
    public void assertNotLocked(String account) {
        String key = normalize(account);
        State state = states.get(key);
        if (state == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (state.lockedUntil > now) {
            long remainMillis = state.lockedUntil - now;
            long minutes = Math.max(1, (remainMillis + 59_999) / 60_000);
            throw new BusinessException(429, "密码错误次数过多，请 " + minutes + " 分钟后再试");
        }
        if (state.lockedUntil > 0 || now - state.windowStart > windowMillis) {
            // 锁定已过期，或计数窗口已失效
            states.remove(key);
        }
    }

    /** 记一次失败；返回是否刚刚触发锁定 */
    public boolean recordFailure(String account) {
        State state = states.computeIfAbsent(normalize(account), k -> new State());
        synchronized (state) {
            long now = System.currentTimeMillis();
            if (state.lockedUntil > now) {
                return true;
            }
            if (state.lockedUntil > 0 || state.windowStart == 0 || now - state.windowStart > windowMillis) {
                state.failures = 0;
                state.lockedUntil = 0;
                state.windowStart = now;
            }
            state.failures++;
            if (state.failures >= maxFailures) {
                state.failures = 0;
                state.windowStart = 0;
                state.lockedUntil = now + lockMillis;
                return true;
            }
            return false;
        }
    }

    /** 登录成功，清空计数 */
    public void reset(String account) {
        states.remove(normalize(account));
    }

    private String normalize(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }
}
