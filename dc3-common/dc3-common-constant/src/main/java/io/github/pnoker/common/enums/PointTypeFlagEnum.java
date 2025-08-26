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
 * 通用位号类型标识枚举
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum PointTypeFlagEnum {
    /**
     * 字符串
     */
    STRING((byte) 0, "string", "字符串"),

    /**
     * 字节
     */
    BYTE((byte) 1, "byte", "字节"),

    /**
     * 短整数
     */
    SHORT((byte) 2, "short", "短整数"),

    /**
     * 整数
     */
    INT((byte) 3, "int", "整数"),

    /**
     * 长整数
     */
    LONG((byte) 4, "long", "长整数"),

    /**
     * 浮点数
     */
    FLOAT((byte) 5, "float", "浮点数"),

    /**
     * 双精度浮点数
     */
    DOUBLE((byte) 6, "double", "双精度浮点数"),

    /**
     * 布尔量
     */
    BOOLEAN((byte) 7, "boolean", "布尔量"),
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
    private final String remark;

    /**
     * 根据枚举索引获取枚举
     *
     * @param index 索引
     * @return {@link PointTypeFlagEnum}
     */
    public static PointTypeFlagEnum ofIndex(Byte index) {
        Optional<PointTypeFlagEnum> any = Arrays.stream(PointTypeFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link PointTypeFlagEnum}
     */
    public static PointTypeFlagEnum ofCode(String code) {
        Optional<PointTypeFlagEnum> any = Arrays.stream(PointTypeFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link PointTypeFlagEnum}
     */
    public static PointTypeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
