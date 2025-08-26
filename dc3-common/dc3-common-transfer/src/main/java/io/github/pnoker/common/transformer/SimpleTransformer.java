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

package io.github.pnoker.common.transformer;

import io.github.pnoker.common.annotation.Transform;
import org.springframework.lang.NonNull;


/**
 * 简单转换器接口
 * 自定义转换场景中如果自定义注解里不需要额外属性(除开from属性), 直接实现该接口即可
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface SimpleTransformer<T> extends Transformer<T, Transform> {
    /**
     * 转换
     *
     * @param originalValue 转换之前的原始值
     * @param transform     注解
     * @return 转换后的值
     */
    @Override
    default String transform(@NonNull T originalValue, @NonNull Transform transform) {
        return transform(originalValue);
    }

    /**
     * 转换
     *
     * @param originalValue 原始值
     * @return 转换后的值
     */
    String transform(@NonNull T originalValue);


}
