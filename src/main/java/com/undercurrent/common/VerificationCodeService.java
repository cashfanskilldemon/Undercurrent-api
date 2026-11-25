package com.undercurrent.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码：生成、发送、校验、限流。邮箱与短信共用同一套规则，只是发送通道不同。
 * <p>存内存（重启即失效）；多实例部署换 Redis：key 用 scene:target。
 * <p>规则：5 分钟有效、一次性、错 5 次作废；同一目标 60 秒内不可重发；每天最多 10 条。
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeService {

    public static final String SCENE_REGISTER = "register";
    public static final String SCENE_RESET = "reset";

    /** 发送通道 */
    public enum Channel {
        EMAIL, SMS
    }

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

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    @Value("${app.code.ttl-minutes:5}")
    private int ttlMinutes;

    @Value("${app.code.resend-seconds:60}")
    private int resendSeconds;

    @Value("${app.code.daily-limit:10}")
    private int dailyLimit;

    @Value("${app.code.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final Map<String, Quota> dailyQuota = new ConcurrentHashMap<>();

    /** 发送验证码，返回明文码（是否下发给前端由调用方决定） */
    public String send(String target, String scene, Channel channel) {
        String key = key(target, scene);
        long now = System.currentTimeMillis();

        Entry existing = codes.get(key);
        if (existing != null && now - existing.sentAt < resendSeconds * 1000L) {
            long wait = (resendSeconds * 1000L - (now - existing.sentAt) + 999) / 1000;
            throw new BusinessException(429, "发送太频繁，请 " + wait + " 秒后再试");
        }
        checkDailyQuota(target);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Entry entry = new Entry();
        entry.code = code;
        entry.expireAt = now + ttlMinutes * 60_000L;
        entry.attempts = 0;
        entry.sentAt = now;
        codes.put(key, entry);

        if (channel == Channel.EMAIL) {
            emailSender.sendCode(target, code, scene);
        } else {
            smsSender.send(target, code, scene);
        }
        return code;
    }

    /** 校验验证码，通过后立即失效 */
    public void verify(String target, String scene, String code) {
        String key = key(target, scene);
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

    private void checkDailyQuota(String target) {
        LocalDate today = LocalDate.now();
        Quota quota = dailyQuota.computeIfAbsent(target, t -> {
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

    private String key(String target, String scene) {
        return scene + ":" + target;
    }
}
