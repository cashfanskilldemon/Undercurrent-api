package com.undercurrent.common;

/**
 * 短信发送器。当前只有 dev 实现（打日志 + 回传给前端方便联调），
 * 接入阿里云 / 腾讯云时新增一个实现类并加 @Primary 即可，业务代码不用动。
 */
public interface SmsSender {

    /** 发送验证码；失败抛异常 */
    void send(String phone, String code, String scene);
}
