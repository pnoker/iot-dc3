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
 * 通用驱动事件枚举
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum DeviceCommandTypeEnum {
    /**
     * 读位号值类型指令
     */
    READ((byte) 0, "read", "读位号值类型指令"),

    /**
     * 批量读位号值类型指令
     */
    READ_BATCH((byte) 1, "read_batch", "批量读位号值类型指令"),

    /**
     * 写位号值类型指令
     */
    WRITE((byte) 2, "write", "写位号值类型指令"),

    /**
     * 批量写位号值类型指令
     */
    WRITE_BATCH((byte) 3, "write_batch", "批量写位号值类型指令"),

    /**
     * 配置设备类型指令
     */
    CONFIG((byte) 4, "config", "配置设备类型指令"),
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
     * @return {@link DeviceCommandTypeEnum}
     */
    public static DeviceCommandTypeEnum ofIndex(Byte index) {
        Optional<DeviceCommandTypeEnum> any = Arrays.stream(DeviceCommandTypeEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link DeviceCommandTypeEnum}
     */
    public static DeviceCommandTypeEnum ofCode(String code) {
        Optional<DeviceCommandTypeEnum> any = Arrays.stream(DeviceCommandTypeEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link DeviceCommandTypeEnum}
     */
    public static DeviceCommandTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
