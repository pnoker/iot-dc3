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
 * 通用模板共享类型标识枚举
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum ProfileShareFlagEnum {
    /**
     * 租户下共享模板
     */
    TENANT((byte) 0, "tenant", "租户下共享模板"),

    /**
     * 驱动下共享模板
     */
    DRIVER((byte) 1, "driver", "驱动下共享模板"),

    /**
     * 用户下共享模板
     */
    USER((byte) 2, "user", "用户下共享模板"),
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
     * @return {@link ProfileShareFlagEnum}
     */
    public static ProfileShareFlagEnum ofIndex(Byte index) {
        Optional<ProfileShareFlagEnum> any = Arrays.stream(ProfileShareFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link ProfileShareFlagEnum}
     */
    public static ProfileShareFlagEnum ofCode(String code) {
        Optional<ProfileShareFlagEnum> any = Arrays.stream(ProfileShareFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link ProfileShareFlagEnum}
     */
    public static ProfileShareFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
