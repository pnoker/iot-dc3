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
 * 通用失效类型枚举
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum ExpireFlagEnum {
    /**
     * 永久
     */
    PERMANENT((byte) 0, "permanent", "永久"),

    /**
     * 一天
     */
    ONE_DAY((byte) 1, "one_day", "一天"),

    /**
     * 一周
     */
    ONE_WEEK((byte) 2, "one_week", "一周"),

    /**
     * 一月
     */
    ONE_MONTH((byte) 3, "one_month", "一月"),

    /**
     * 三月
     */
    THREE_MONTHS((byte) 4, "three_months", "三月"),

    /**
     * 半年
     */
    HALF_YEAR((byte) 5, "half_year", "半年"),

    /**
     * 一年
     */
    ONE_YEAR((byte) 6, "one_year", "一年"),

    /**
     * 自定义
     */
    CUSTOM((byte) 7, "custom", "自定义"),
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
     * @return {@link ExpireFlagEnum}
     */
    public static ExpireFlagEnum ofIndex(Byte index) {
        Optional<ExpireFlagEnum> any = Arrays.stream(ExpireFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return {@link ExpireFlagEnum}
     */
    public static ExpireFlagEnum ofCode(String code) {
        Optional<ExpireFlagEnum> any = Arrays.stream(ExpireFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param name 枚举内容
     * @return {@link ExpireFlagEnum}
     */
    public static ExpireFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
