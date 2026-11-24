package com.undercurrent.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 无状态登录凭证：HMAC-SHA256 签名的 {@code base64url(uid.exp).base64url(签名)}。
 * 不落库、不依赖 Redis，改密钥即全部失效。生产环境务必用 AUTH_SECRET 覆盖默认值。
 */
@Component
public class TokenService {

    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long ttlSeconds;

    public TokenService(
            @Value("${app.auth.secret:undercurrent-dev-secret-change-me}") String secret,
            @Value("${app.auth.ttl-seconds:604800}") long ttlSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.auth.secret 未配置，请设置 AUTH_SECRET 环境变量");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    /** 签发：payload 里带用户 id 和过期时间戳 */
    public String issue(Long userId) {
        long exp = System.currentTimeMillis() / 1000 + ttlSeconds;
        String payload = userId + "." + exp;
        String encoded = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + ENCODER.encodeToString(sign(encoded));
    }

    /** 校验并解出用户 id；签名不对 / 过期 / 格式错都返回 null */
    public Long verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return null;
        }
        try {
            String encoded = token.substring(0, dot);
            byte[] signature = DECODER.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(sign(encoded), signature)) {
                return null;
            }
            String[] parts = new String(DECODER.decode(encoded), StandardCharsets.UTF_8).split("\\.");
            if (parts.length != 2) {
                return null;
            }
            long exp = Long.parseLong(parts[1]);
            if (exp * 1000 <= System.currentTimeMillis()) {
                return null;
            }
            return Long.parseLong(parts[0]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private byte[] sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("签发 token 失败", e);
        }
    }
}
