package com.undercurrent.common;

/** 邮件发送器。dev 只打日志，smtp 用真实邮箱发。 */
public interface EmailSender {

    /** 发送验证码邮件；失败抛异常 */
    void sendCode(String email, String code, String scene);
}
