package com.undercurrent.common;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * SMTP 发信：app.email.provider=smtp 时启用。
 * <p>QQ / 163 邮箱需要「授权码」而非登录密码，配置示例：
 * <pre>
 * app:
 *   email:
 *     provider: smtp
 *     host: smtp.qq.com
 *     port: 465
 *     username: you@qq.com
 *     password: 你的授权码
 *     from: UNDERCURRENT &lt;you@qq.com&gt;
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String appName;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.email.from:}") String from,
                           @Value("${app.email.app-name:UNDERCURRENT}") String appName) {
        this.mailSender = mailSender;
        this.from = from;
        this.appName = appName;
    }

    @Override
    public void sendCode(String email, String code, String scene) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(subject(scene));
            helper.setText(body(code, scene), false);
            mailSender.send(message);
            log.info("验证码邮件已发送：{} 场景={}", mask(email), scene);
        } catch (Exception e) {
            log.error("验证码邮件发送失败：{}", mask(email), e);
            throw new BusinessException(500, "验证码发送失败，请稍后重试");
        }
    }

    private String subject(String scene) {
        return "reset".equals(scene)
                ? appName + " 重置密码验证码"
                : appName + " 注册验证码";
    }

    private String body(String code, String scene) {
        String action = "reset".equals(scene) ? "重置密码" : "注册账号";
        return "你的" + action + "验证码是：" + code + "\n\n"
                + "5 分钟内有效，请勿转发给他人。\n"
                + "如果不是你本人操作，忽略这封邮件即可。\n\n"
                + appName;
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
