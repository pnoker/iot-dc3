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

package io.github.pnoker.common.transformer;

import io.github.pnoker.common.annotation.Transform;
import org.springframework.lang.NonNull;


/**
 * 简单转换器接口
 * 自定义转换场景中如果自定义注解里不需要额外属性(除开from属性), 直接实现该接口即可
 *
 * @author pnoker
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
