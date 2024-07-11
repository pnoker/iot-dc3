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

package io.github.pnoker.common.annotation;

import io.github.pnoker.common.transformer.Transformer;

import java.lang.annotation.*;

/**
 * 转换注解
 * 最基本的注解, 可以被其他自定义注解衍生
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Transform {

    /**
     * 指定转换器
     */
    Class<? extends Transformer> transformer() default Transformer.class;

    /**
     * 来源字段
     * <p>
     * 默认自动推断(要求转换后字段名必须以Name结尾, 推断规则: 如注解标注的字段是userName, 自动推断结果为"user", "userId"或"userCode")
     */
    String from() default "";
}
