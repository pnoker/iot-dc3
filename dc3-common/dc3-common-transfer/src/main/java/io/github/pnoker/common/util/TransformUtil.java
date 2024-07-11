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

package io.github.pnoker.common.util;

import cn.hutool.core.util.ReflectUtil;
import io.github.pnoker.common.component.TransformClass;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 转换工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@UtilityClass
public class TransformUtil {

    /**
     * 转换类的缓存, 提高性能
     */
    private static final Map<Class<?>, TransformClass> TRANSFORM_CLASS_CACHE = new ConcurrentHashMap<>(512);

    /**
     * 转换对象, 支持集合(Collection)或者单个bean
     * 并直接改变对象内部属性
     *
     * @param object 集合或单个bean
     */
    public static void transform(Object object) throws IllegalAccessException {
        TransformClass transformClass = getTransformClassOfObject(object);
        if (transformClass == null) {
            // 获取不到转换类, 表示object为null或者空集合
            return;
        }
        Set<TransformClass> transformClassRecorder = new HashSet<>();
        // 字段转换, 并记录转换类
        transform(object, transformClass, transformClassRecorder);
        // 清空缓存
        transformClassRecorder.forEach(TransformClass::clearCache);
    }

    /**
     * 对象属性转换, 其中嵌套属性使用递归方式转换
     *
     * @param obj                    需要转换的对象, 集合或者单个bean
     * @param transformClass         转换类
     * @param transformClassRecorder 记录转换类, 为了最后清除缓存
     */
    private void transform(Object obj, TransformClass transformClass, Set<TransformClass> transformClassRecorder) throws IllegalAccessException {
        boolean isNullOrEmptyCollection = obj == null || obj instanceof Collection && ((Collection<?>) obj).isEmpty();
        if (isNullOrEmptyCollection) {
            return;
        }
        // 转换
        transformClass.transform(obj);
        transformClassRecorder.add(transformClass);
        // 处理嵌套转换, 拿到当前类的所有嵌套转换属性和对应的转换类
        Map<Field, Class<?>> nestTransformFields = transformClass.getNestTransformFields();
        Collection<?> collection = (obj instanceof Collection) ? (Collection<?>) obj : Collections.singletonList(obj);
        for (Map.Entry<Field, Class<?>> entry : nestTransformFields.entrySet()) {
            Field field = entry.getKey();
            TransformClass transformClassOfField = getTransformClassFromClass(entry.getValue());
            for (Object bean : collection) {
                // 递归
                Object fieldValue = ReflectUtil.getFieldValue(bean, field);
                transform(fieldValue, transformClassOfField, transformClassRecorder);
            }

        }

    }

    /**
     * 根据class获取TransformClass
     *
     * @param obj obj 对象或者集合, 集合取第一个元素的class
     * @return TransformClass 转换类, 如果是空集合或者空对象, 返回null
     */
    private TransformClass getTransformClassOfObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if ((obj instanceof Collection)) {
            // 集合, 取第一个元素的class, 为空返回null
            Iterator<?> iterator = ((Collection<?>) obj).iterator();
            if (iterator.hasNext()) {
                Class<?> clazz = iterator.next().getClass();
                return getTransformClassFromClass(clazz);
            } else {
                return null;
            }
        }
        return getTransformClassFromClass(obj.getClass());

    }

    public static TransformClass getTransformClassFromClass(Class<?> clazz) {
        return TRANSFORM_CLASS_CACHE.computeIfAbsent(clazz, TransformClass::new);
    }
}

