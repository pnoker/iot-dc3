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

package io.github.pnoker.common.annotation;

import io.github.pnoker.common.transformer.Transformer;

import java.lang.annotation.*;

/**
 * 转换注解
 * 最基本的注解, 可以被其他自定义注解衍生
 *
 * @author pnoker
 * @version 2025.6.0
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
