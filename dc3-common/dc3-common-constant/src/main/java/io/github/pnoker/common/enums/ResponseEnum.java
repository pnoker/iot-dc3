/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
