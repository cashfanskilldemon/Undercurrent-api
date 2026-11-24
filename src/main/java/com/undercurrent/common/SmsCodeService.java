package com.undercurrent.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码：生成、发送、校验、限流。
 * <p>存内存（单机够用，重启即失效）；多实例部署换成 Redis：key 用 scene:phone，value 存 code + 过期时间。
 * <p>规则：
 * <ul>
 *   <li>有效期 5 分钟；</li>
 *   <li>同一手机号 60 秒内不能重复发送；</li>
 *   <li>同一手机号每天最多 10 条；</li>
 *   <li>同一验证码最多尝试 5 次，超过即作废；</li>
 *   <li>校验成功后立刻失效（一次性）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SmsCodeService {

    public static final String SCENE_REGISTER = "register";
    public static final String SCENE_RESET = "reset";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final class Entry {
        String code;
        long expireAt;
        int attempts;
        long sentAt;
    }

    private static final class Quota {
        LocalDate day;
        int count;
    }

    private final SmsSender smsSender;

    @Value("${app.sms.ttl-minutes:5}")
    private int ttlMinutes;

    @Value("${app.sms.resend-seconds:60}")
    private int resendSeconds;

    @Value("${app.sms.daily-limit:10}")
    private int dailyLimit;

    @Value("${app.sms.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final Map<String, Quota> dailyQuota = new ConcurrentHashMap<>();

    /** 发送验证码，返回生成的码（是否需要下发给前端由调用方决定） */
    public String send(String phone, String scene) {
        String key = key(phone, scene);
        long now = System.currentTimeMillis();

        Entry existing = codes.get(key);
        if (existing != null && now - existing.sentAt < resendSeconds * 1000L) {
            long wait = (resendSeconds * 1000L - (now - existing.sentAt) + 999) / 1000;
            throw new BusinessException(429, "发送太频繁，请 " + wait + " 秒后再试");
        }
        checkDailyQuota(phone);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Entry entry = new Entry();
        entry.code = code;
        entry.expireAt = now + ttlMinutes * 60_000L;
        entry.attempts = 0;
        entry.sentAt = now;
        codes.put(key, entry);

        smsSender.send(phone, code, scene);
        return code;
    }

    /** 校验验证码，通过后立即失效 */
    public void verify(String phone, String scene, String code) {
        String key = key(phone, scene);
        Entry entry = codes.get(key);
        if (entry == null) {
            throw new BusinessException(400, "请先获取验证码");
        }
        long now = System.currentTimeMillis();
        if (entry.expireAt <= now) {
            codes.remove(key);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (entry.attempts >= maxVerifyAttempts) {
            codes.remove(key);
            throw new BusinessException(400, "错误次数过多，请重新获取验证码");
        }
        entry.attempts++;
        if (code == null || !entry.code.equals(code.trim())) {
            throw new BusinessException(400, "验证码不正确");
        }
        codes.remove(key);
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    private void checkDailyQuota(String phone) {
        LocalDate today = LocalDate.now();
        Quota quota = dailyQuota.computeIfAbsent(phone, p -> {
            Quota q = new Quota();
            q.day = today;
            return q;
        });
        synchronized (quota) {
            if (!today.equals(quota.day)) {
                quota.day = today;
                quota.count = 0;
            }
            if (quota.count >= dailyLimit) {
                throw new BusinessException(429, "今日验证码发送次数已达上限，请明天再试");
            }
            quota.count++;
        }
    }

    private String key(String phone, String scene) {
        return scene + ":" + phone;
    }
}
