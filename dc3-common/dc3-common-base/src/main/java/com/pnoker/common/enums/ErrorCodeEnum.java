/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pnoker.common.enums;

import lombok.Getter;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: The class Error code enum.
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
