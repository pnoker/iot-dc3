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

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;


/**
 * 解包器
 * <p>
 * 当方法返回值是包装类(如Page, ResultWrapper等)时, 指定解包的逻辑
 * 注意解包之后的返回参数必须是某个bean或者集合类型
 *
 * @author pnoker
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
