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
 * 通用权限类型枚举
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeFlagEnum {
    /**
     * 驱动
     */
    DRIVER((byte) 0, "driver", "驱动"),

    /**
     * 模版
     */
    PROFILE((byte) 1, "profile", "模版"),

    /**
     * 位号
     */
    POINT((byte) 2, "point", "位号"),

    /**
     * 设备
     */
    DEVICE((byte) 3, "device", "设备"),

    /**
     * 数据
     */
    DATA((byte) 4, "data", "数据"),

    /**
     * 菜单
     */
    MENU((byte) 5, "menu", "菜单"),

    /**
     * 接口
     */
    API((byte) 6, "api", "接口"),
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
     * @return {@link ResourceTypeFlagEnum}
     */
    public static ResourceTypeFlagEnum ofIndex(Byte index) {
        Optional<ResourceTypeFlagEnum> any = Arrays.stream(ResourceTypeFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link ResourceTypeFlagEnum}
     */
    public static ResourceTypeFlagEnum ofCode(String code) {
        Optional<ResourceTypeFlagEnum> any = Arrays.stream(ResourceTypeFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link ResourceTypeFlagEnum}
     */
    public static ResourceTypeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
