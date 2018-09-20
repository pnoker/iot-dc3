package com.pnoker.common.util.base.enums;

import lombok.Getter;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: iot-dc3
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>
 * The class Error code enum.
 */
@Getter
public enum ErrorCodeEnum {

    /**
     * 全局 100 参数异常
     */
    Global100(100, "参数异常"),
    /**
     * 全局 101 注解使用错误
     */
    Global101(101, "注解使用错误"),
    /**
     * 全局 102 微服务不在线,或者网络超时
     */
    Global102(102, "微服务不在线,或者网络超时"),
    /**
     * 全局 401 无访问权限
     */
    Global401(401, "无访问权限"),
    /**
     * 全局 500 未知异常
     */
    Global500(500, "未知异常"),
    /**
     * 全局 403 无权访问
     */
    Global403(403, "无权访问"),
    /**
     * 全局 404 找不到指定资源
     */
    Global404(404, "找不到指定资源");
    private int code;
    private String msg;


    ErrorCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * Gets enum.
     *
     * @param code the code
     * @return the enum
     */
    public static ErrorCodeEnum getEnum(int code) {
        for (ErrorCodeEnum errorCodeEnum : ErrorCodeEnum.values()) {
            if (errorCodeEnum.getCode() == code) {
                return errorCodeEnum;
            }
        }
        return null;
    }
}
