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

package io.github.pnoker.common.base.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 基础枚举
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface BaseEnum {

    /**
     * 根据枚举索引获取枚举
     *
     * @param index 索引
     * @return 枚举
     */
    static <E extends BaseEnum> E ofIndex(Class<E> clazz, Byte index) {
        Optional<E> first = Arrays.stream(clazz.getEnumConstants()).filter(type -> type.getIndex().equals(index)).findFirst();
        return first.orElse(null);
    }

    /**
     * 根据枚举编码获取枚举
     *
     * @param code 编码
     * @return 枚举
     */
    static <E extends BaseEnum> E ofCode(Class<E> clazz, String code) {
        Optional<E> first = Arrays.stream(clazz.getEnumConstants()).filter(type -> type.getCode().equals(code)).findFirst();
        return first.orElse(null);
    }

    /**
     * 根据枚举内容获取枚举
     *
     * @param text 枚举内容
     * @return 枚举
     */
    static <E extends BaseEnum> E ofName(Class<E> clazz, String text) {
        Optional<E> first = Arrays.stream(clazz.getEnumConstants()).filter(type -> type.getText().equals(text)).findFirst();
        return first.orElse(null);
    }

    /**
     * 初始化
     *
     * @param index 索引
     * @param code  编码
     * @param text  内容
     */
    default void init(Byte index, String code, String text) {
        EnumPool.put(this, index, code, text);
    }

    /**
     * 获取索引
     *
     * @return 索引
     */
    default Byte getIndex() {
        return EnumPool.get(this).getIndex();
    }

    /**
     * 获取编码
     *
     * @return 编码
     */
    default String getCode() {
        return EnumPool.get(this).getCode();
    }

    /**
     * 获取内容
     *
     * @return 字典内容
     */
    default String getText() {
        return EnumPool.get(this).getText();
    }
}
