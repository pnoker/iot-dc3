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

package io.github.pnoker.common.component;

import cn.hutool.core.util.ReflectUtil;
import io.github.pnoker.common.annotation.Transform;
import lombok.Data;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 标记为@Transform的类的封装
 * 缓存相关信息, 提高性能
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class TransformClass {

    private Class<?> clazz;
    /**
     * 需要转换的字段集合
     */
    private List<TransformField<?>> transformFields = new ArrayList<>();
    /**
     * 需要嵌套转换的字段及其对应的转换类
     */
    private Map<Field, Class<?>> nestTransformFields = new HashMap<>();

    public TransformClass(Class<?> clazz) {
        this.clazz = clazz;
        Field[] allFields = ReflectUtil.getFields(clazz);
        for (Field field : allFields) {
            if (field.getType() == String.class && AnnotatedElementUtils.isAnnotated(field, Transform.class)) {
                // 需要转换的字段, 字段上的注解链上需要有@Transform, 且字段类型必须为String
                transformFields.add(new TransformField<>(field));
            } else if (field.getType() != String.class && field.isAnnotationPresent(Transform.class)) {
                // 需要嵌套转换的字段, 类型不为String, 且字段上直接标注了@Transform
                nestTransformFields.put(field, getClassFromField(field));
            }
        }
    }

    /**
     * 转换本类所有需要转换的字段(不含嵌套转换)
     *
     * @param obj 对象或集合
     */
    public void transform(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return;
        }
        if (obj instanceof Collection) {
            for (Object bean : (Collection<?>) obj) {
                transformBean(bean);
            }
        } else {
            transformBean(obj);
        }
    }

    /**
     * 转换单个bean
     *
     * @param bean bean
     */
    private void transformBean(Object bean) throws IllegalAccessException {
        for (TransformField<?> transformField : transformFields) {
            transformField.transform(bean);
        }
    }

    /**
     * 清空转换缓存
     */
    public void clearCache() {
        // 清空字段转换缓存
        transformFields.forEach(TransformField::clearCache);
    }


    private Class<?> getClassFromField(Field field) {
        Class<?> fieldType = field.getType();
        if (Collection.class.isAssignableFrom(fieldType)) {
            // 集合类型, 获取泛型
            Class<?> genericClass = ResolvableType.forField(field).getGeneric(0).resolve();
            Assert.notNull(genericClass, "属性" + field.getName() + "的List上的泛型不能为空");
            return genericClass;
        }
        return fieldType;
    }
}
