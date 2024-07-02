/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 通用返回结果枚举
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum ResponseEnum {
    OK((byte) 200, "R200", "Success"),
    TOKEN_INVALID((byte) 20301, "R20301", "Token is invalid"),
    IP_INVALID((byte) 20302, "R20302", "Invalid IP"),
    FAILURE((byte) 500, "R500", "Service exception"),
    NO_RESOURCE((byte) 404, "R404", "Resource does not exist"),
    OUT_RANGE((byte) 900, "R900", "Number out of range"),

    ADD_SUCCESS((byte) 20001, "R20001", "Added successfully"),
    DELETE_SUCCESS((byte) 20002, "R20002", "Deleted successfully"),
    UPDATE_SUCCESS((byte) 20003, "R20003", "Updated successfully"),
    ;

    /**
     * 索引
     */
    @EnumValue
    private final Byte index;

    /**
     * 编码
     */
    private final String code;

    /**
     * 内容
     */
    private final String text;

    /**
     * 根据枚举索引获取枚举
     *
     * @param index 索引
     * @return {@link ResponseEnum}
     */
    public static ResponseEnum ofIndex(Byte index) {
        Optional<ResponseEnum> any = Arrays.stream(ResponseEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link ResponseEnum}
     */
    public static ResponseEnum ofCode(String code) {
        Optional<ResponseEnum> any = Arrays.stream(ResponseEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link ResponseEnum}
     */
    public static ResponseEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
