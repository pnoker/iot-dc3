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

package io.github.pnoker.common.optional;

import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 自定义 Collection Optional
 *
 * @author pnoker
 * @version 2025.6.0
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
