package com.undercurrent.config;

import com.undercurrent.common.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 邮件通道自检：配置 app.email.test-to 后，启动时发一封测试邮件并在日志里给出结果。
 * 只在 app.email.provider=smtp 时才有意义；dev 模式下不会触发。
 * <p>用法：
 * <pre>
 * app:
 *   email:
 *     provider: smtp
 *     test-to: you@example.com   # 配了才自检，不配就不发
 * </pre>
 */
@Component
public class EmailStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailStartupCheck.class);

    private final EmailSender emailSender;
    private final String testTo;

    public EmailStartupCheck(EmailSender emailSender,
                             @Value("${app.email.test-to:}") String testTo) {
        this.emailSender = emailSender;
        this.testTo = testTo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (testTo == null || testTo.isBlank()) {
            return;
        }
        long started = System.currentTimeMillis();
        try {
            emailSender.sendCode(testTo.trim(), "123456", "register");
            log.info("邮件自检成功：已向 {} 发送测试邮件，耗时 {} ms（验证码 123456 是假的，别当真）",
                    testTo.trim(), System.currentTimeMillis() - started);
        } catch (Exception e) {
            log.error("邮件自检失败：{} —— 检查 spring.mail.host/username/password，"
                    + "QQ / 腾讯企业邮要用授权码或客户端专用密码", e.getMessage());
        }
    }
}
