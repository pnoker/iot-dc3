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

package io.github.pnoker.common.driver.entity.bo;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.TypeException;
import io.github.pnoker.common.exception.UnSupportException;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 属性配置
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AttributeBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 值, string, 需要根据type确定真实的数据类型
     */
    private String value;

    /**
     * 类型, value type, 用于确定value的真实类型
     */
    private AttributeTypeFlagEnum type;

    /**
     * 根据类型转换数据
     *
     * @param clazz T Class
     * @param <T>   T
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> clazz) {
        if (Objects.isNull(type)) {
            throw new UnSupportException("Unsupported attribute type of " + type);
        }
        if (CharSequenceUtil.isEmpty(value)) {
            throw new EmptyException("Attribute value is empty");
        }

        final String message = "Attribute type is: {}, can't be cast to class: {}";
        return switch (type) {
            case STRING -> {
                if (!clazz.equals(String.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) value;
            }
            case BYTE -> {
                if (!clazz.equals(Byte.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Byte.valueOf(value);
            }
            case SHORT -> {
                if (!clazz.equals(Short.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Short.valueOf(value);
            }
            case INT -> {
                if (!clazz.equals(Integer.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Integer.valueOf(value);
            }
            case LONG -> {
                if (!clazz.equals(Long.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Long.valueOf(value);
            }
            case FLOAT -> {
                if (!clazz.equals(Float.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Float.valueOf(value);
            }
            case DOUBLE -> {
                if (!clazz.equals(Double.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Double.valueOf(value);
            }
            case BOOLEAN -> {
                if (!clazz.equals(Boolean.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Boolean.valueOf(value);
            }
        };
    }
}
