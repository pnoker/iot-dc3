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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举池, 所有Dict的实现类都会在这里注册
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface EnumPool {

    /**
     * 存储所有枚举
     */
    Map<BaseEnum, EnumBean> ENUM_MAP = new ConcurrentHashMap<>();

    /**
     * 枚举类和对应的枚举集合
     */
    Map<Class<?>, List<EnumBean>> ENUM_CLASS_ITEMS_MAP = new ConcurrentHashMap<>();

    /**
     * 获取枚举
     */
    static EnumBean get(BaseEnum dict) {
        return ENUM_MAP.get(dict);
    }

    /**
     * 保存枚举
     */
    static void put(BaseEnum dict, Byte index, String code, String text) {
        Class<?> dictClass = dict.getClass();
        EnumBean dictBean = new EnumBean(index, code, text);
        ENUM_MAP.put(dict, dictBean);
        ENUM_CLASS_ITEMS_MAP.computeIfAbsent(dictClass, k -> new ArrayList<>()).add(dictBean);
    }

    /**
     * 获取枚举集合
     */
    static List<EnumBean> getAll(Class<? extends BaseEnum> clazz) {
        clazz.getEnumConstants();
        return ENUM_CLASS_ITEMS_MAP.get(clazz);
    }
}
