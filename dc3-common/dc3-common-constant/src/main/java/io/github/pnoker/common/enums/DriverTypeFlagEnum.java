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
 * 通用驱动类型标识枚举
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum DriverTypeFlagEnum {
    /**
     * 协议驱动, 客户端模式
     */
    DRIVER_CLIENT((byte) 0, "driver_client", "协议类型驱动, 客户端模式"),

    /**
     * 协议驱动,  服务端模式
     */
    DRIVER_SERVER((byte) 1, "driver_server", "协议类型驱动, 服务端模式"),

    /**
     * 网关驱动
     */
    GATEWAY((byte) 2, "gateway", "网关类型驱动"),

    /**
     * 串联驱动
     */
    CONNECT((byte) 3, "connect", "串联类型驱动"),
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
     * @return {@link DriverTypeFlagEnum}
     */
    public static DriverTypeFlagEnum ofIndex(Byte index) {
        Optional<DriverTypeFlagEnum> any = Arrays.stream(DriverTypeFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link DriverTypeFlagEnum}
     */
    public static DriverTypeFlagEnum ofCode(String code) {
        Optional<DriverTypeFlagEnum> any = Arrays.stream(DriverTypeFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link DriverTypeFlagEnum}
     */
    public static DriverTypeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
