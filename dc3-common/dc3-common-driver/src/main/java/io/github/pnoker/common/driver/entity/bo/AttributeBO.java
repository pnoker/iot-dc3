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
 * @version 2025.6.0
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
