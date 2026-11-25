package com.undercurrent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发用邮件发送器：不真的发信，只把验证码打到后端日志，接口同时回传给前端。
 * 配置 app.email.provider=smtp 后本类失效，改用 SmtpEmailSender。
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "dev", matchIfMissing = true)
public class DevEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(DevEmailSender.class);

    @Override
    public void sendCode(String email, String code, String scene) {
        log.warn("[EMAIL-DEV] 场景={} 收件人={} 验证码={} （未真实发送，仅打印）", scene, email, code);
    }
}
