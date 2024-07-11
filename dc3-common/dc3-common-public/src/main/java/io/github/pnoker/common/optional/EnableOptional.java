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

import io.github.pnoker.common.enums.EnableFlagEnum;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 自定义 Enable Optional
 *
 * @author pnoker
 * @since 2022.1.0
 */
public final class EnableOptional {

    private final EnableFlagEnum value;

    private EnableOptional(byte index) {
        this.value = EnableFlagEnum.ofIndex(index);
    }

    private EnableOptional(int index) {
        this.value = EnableFlagEnum.ofIndex((byte) index);
    }

    public static EnableOptional ofNullable(byte index) {
        return new EnableOptional(index);
    }

    public static EnableOptional ofNullable(int index) {
        return new EnableOptional(index);
    }

    public void ifPresent(Consumer<EnableFlagEnum> action) {
        if (Objects.nonNull(value)) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<EnableFlagEnum> action, Runnable emptyAction) {
        if (Objects.nonNull(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }
}
