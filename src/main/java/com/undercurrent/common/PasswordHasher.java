package com.undercurrent.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希。
 * <p>主算法：BCrypt（cost 12），库里的串以 {@code $2a$} 开头。
 * <p>兼容：早期 PBKDF2-HMAC-SHA256 的 {@code pbkdf2$迭代$盐$哈希} 仍可校验，
 * 登录成功时由 Service 依据 {@link #needsUpgrade} 顺手重算成 BCrypt。
 */
public final class PasswordHasher {

    /** BCrypt 代价因子：12 ≈ 250ms/次，比默认 10 更抗爆破 */
    private static final int BCRYPT_COST = 12;
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(BCRYPT_COST);

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** 生成可直接存库的哈希串（BCrypt，每次自动带随机盐） */
    public static String hash(String raw) {
        return BCRYPT.encode(raw);
    }

    /** 校验明文密码与库里的哈希串是否匹配 */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (isBcrypt(stored)) {
            return BCRYPT.matches(raw, stored);
        }
        return matchesLegacyPbkdf2(raw, stored);
    }

    /** 是否需要重算：旧格式、或 BCrypt 代价因子低于当前配置 */
    public static boolean needsUpgrade(String stored) {
        if (stored == null || stored.isEmpty() || !isBcrypt(stored)) {
            return true;
        }
        try {
            return Integer.parseInt(stored.substring(4, 6)) < BCRYPT_COST;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static boolean isBcrypt(String stored) {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }

    /** 旧格式 pbkdf2$迭代次数$盐(base64)$哈希(base64) */
    private static boolean matchesLegacyPbkdf2(String raw, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PBKDF2_PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, expected.length * 8);
            byte[] actual = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
            // 定长比较，避免时序侧信道
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    /** 生成随机盐的 PBKDF2 哈希，仅用于造兼容测试数据 */
    public static String hashLegacyPbkdf2(String raw) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, 120_000, 256);
            byte[] derived = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
            return PBKDF2_PREFIX + "$120000$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(derived);
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希失败", e);
        }
    }
}
