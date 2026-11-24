package com.undercurrent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发用短信发送器：不真的发短信，只把验证码打到后端日志。
 * 接入真实短信服务后，把 app.sms.provider 改成 aliyun / tencent 即可让本类失效。
 */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "dev", matchIfMissing = true)
public class DevSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(DevSmsSender.class);

    @Override
    public void send(String phone, String code, String scene) {
        log.warn("[SMS-DEV] 场景={} 手机号={} 验证码={} （未真实发送，仅打印）", scene, phone, code);
    }
}
