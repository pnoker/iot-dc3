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

package io.github.pnoker.common.optional;

import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 自定义 Collection Optional
 *
 * @author pnoker
 * @since 2022.1.0
 */
public final class CollectionOptional<T> {

    private final Collection<T> value;

    private CollectionOptional(Collection<T> value) {
        this.value = value;
    }

    public static <T> CollectionOptional<T> ofNullable(Collection<T> value) {
        return new CollectionOptional<>(value);
    }

    public void ifPresent(Consumer<Collection<T>> action) {
        if (CollUtil.isNotEmpty(value)) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<Collection<?>> action, Runnable emptyAction) {
        if (CollUtil.isNotEmpty(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }
}
