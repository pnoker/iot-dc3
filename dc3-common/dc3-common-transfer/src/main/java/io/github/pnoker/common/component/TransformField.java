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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import io.github.pnoker.common.annotation.Transform;
import io.github.pnoker.common.transformer.Transformer;
import io.github.pnoker.common.util.SpringContextUtil;
import lombok.Data;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标记为@Transform的属性的封装
 * 缓存相关信息, 提高性能
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class TransformField<T> {
    private Field field;
    private Field originField;
    private Transformer<T, Annotation> transformer;
    private Annotation transformAnnotation;
    /**
     * 转换结果缓存, 线程级别
     */
    private ThreadLocal<Map<T, String>> transformResultCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @SuppressWarnings("unchecked")
    public TransformField(Field field) {
        this.field = field;
        // 搜索属性上所有注解继承关系, 获取最终合并注解属性后的@Transform注解实例(主要是拿到from和transformer两个属性)
        Transform mergedAnnotation = AnnotatedElementUtils.getMergedAnnotation(field, Transform.class);
        Assert.notNull(mergedAnnotation, "字段" + field.getName() + "上必须标注@Transform注解或其衍生注解");
        String originFieldName = mergedAnnotation.from().isEmpty() ? analyzeOriginFieldName(field) : mergedAnnotation.from();
        this.originField = ReflectUtil.getField(field.getDeclaringClass(), originFieldName);
        Class<? extends Transformer<T, Annotation>> transformerClass = (Class<? extends Transformer<T, Annotation>>) mergedAnnotation.transformer();
        // 根据spring容器拿转换器实例
        this.transformer = SpringContextUtil.getBean(transformerClass);
        // 获取自定义注解类型(Transformer上有两个泛型, 第一个是转换前的值类型, 第二个是是自定义注解类型)
        ResolvableType resolvableType = ResolvableType.forClass(Transformer.class, transformerClass);
        Class<? extends Annotation> customTransformAnnotationType = (Class<? extends Annotation>) resolvableType.getGeneric(1).resolve();
        Assert.notNull(customTransformAnnotationType, "实现Transform接口时必须指定泛型: " + transformer.getClass().getSimpleName());
        this.transformAnnotation = field.getAnnotation(customTransformAnnotationType);
    }

    /**
     * 转换结果, 并缓存
     *
     * @param bean 对象
     */
    @SuppressWarnings("unchecked")
    public void transform(Object bean) throws IllegalAccessException {
        T originalValue = (T) ReflectUtil.getFieldValue(bean, originField);
        if (originalValue == null) {
            return;
        }
        String transformResult = transformResultCache.get().computeIfAbsent(originalValue, k -> transformer.transform(originalValue, transformAnnotation));
        ReflectUtil.setFieldValue(bean, field, transformResult);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        transformResultCache.remove();
    }

    /**
     * 根据当前字段, 智能分析原字段名称
     */
    private String analyzeOriginFieldName(Field field) {
        Class<?> beanClass = field.getDeclaringClass();
        String fieldName = field.getName();
        // 没设置from值, 智能推断关联的属性名, 可能为xx, xxId, xxCode
        String possibleNameA = CharSequenceUtil.replace(fieldName, "Name", "");
        String possibleNameB = CharSequenceUtil.replace(fieldName, "Name", "Id");
        String possibleNameC = CharSequenceUtil.replace(fieldName, "Name", "Code");
        List<String> possibleNameList = Arrays.asList(possibleNameA, possibleNameB, possibleNameC);
        // 匹配bean属性列表
        for (Field beanField : ReflectUtil.getFields(beanClass)) {
            if (possibleNameList.contains(beanField.getName())) {
                return beanField.getName();
            }
        }
        throw new IllegalArgumentException("转换异常: 无法自动推断" + fieldName + "的原始字段名, 请使用注解@Transform的from属性指定被转换的字段");
    }
}
