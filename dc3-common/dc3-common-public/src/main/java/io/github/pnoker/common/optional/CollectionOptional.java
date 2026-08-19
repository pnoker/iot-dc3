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

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Optional wrapper for {@link java.util.Collection} with null/empty checks.
 *
 * @param <T> collection element type
 * @author pnoker
 * @since 2016.10.1
 */
public final class CollectionOptional<T> {

    private final Collection<T> value;

    private CollectionOptional(Collection<T> value) {
        this.value = value;
    }

    /**
     * Create a wrapper that treats {@code null} and empty collections as absent.
     *
     * @param value collection to wrap
     * @param <T> collection element type
     * @return wrapper for the supplied collection
     */
    public static <T> CollectionOptional<T> ofNullable(Collection<T> value) {
        return new CollectionOptional<>(value);
    }

    /**
     * Invoke the action when the wrapped collection is not empty.
     *
     * @param action action that consumes the present collection
     */
    public void ifPresent(Consumer<Collection<T>> action) {
        if (CollectionUtils.isNotEmpty(value)) {
            action.accept(value);
        }
    }

    /**
     * Invoke exactly one branch according to whether the wrapped collection is empty.
     *
     * @param action action that consumes a present collection
     * @param emptyAction action to run for a {@code null} or empty collection
     */
    public void ifPresentOrElse(Consumer<Collection<?>> action, Runnable emptyAction) {
        if (CollectionUtils.isNotEmpty(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

}
