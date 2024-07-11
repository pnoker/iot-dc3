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

package io.github.pnoker.common.base.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举池, 所有Dict的实现类都会在这里注册
 *
 * @author pnoker
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
