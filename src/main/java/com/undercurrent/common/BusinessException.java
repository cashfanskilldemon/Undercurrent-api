package com.undercurrent.common;

import lombok.Getter;

/**
 * 业务异常。由全局异常处理器转成统一响应体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(404, what + "不存在");
    }
}
