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

package io.github.pnoker.common.component;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;


/**
 * 解包器
 * <p>
 * 当方法返回值是包装类(如Page, ResultWrapper等)时, 指定解包的逻辑
 * 注意解包之后的返回参数必须是某个bean或者集合类型
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface UnWrapper<T> extends Converter<T, Object> {

    /**
     * 解包
     *
     * @param source 源
     * @return 包装类内的实际对象
     */
    Object unWrap(T source);

    /**
     * 将convert更名为unWrap
     *
     * @param source 源
     * @return 目标对象
     */
    @Override
    default Object convert(@NonNull T source) {
        return unWrap(source);
    }
}
