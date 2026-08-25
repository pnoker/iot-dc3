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

import io.github.pnoker.common.enums.EnableFlagEnum;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Optional wrapper for enable-flag enumeration values.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public final class EnableOptional {

    private final EnableFlagEnum value;

    private EnableOptional(byte index) {
        this.value = EnableFlagEnum.ofIndex(index);
    }

    private EnableOptional(int index) {
        this.value = EnableFlagEnum.ofIndex((byte) index);
    }

    /**
     * Create a wrapper from a persisted byte index.
     *
     * @param index enable-flag index
     * @return wrapper containing the resolved flag, if known
     */
    public static EnableOptional ofNullable(byte index) {
        return new EnableOptional(index);
    }

    /**
     * Create a wrapper from an integer index after narrowing it to the persisted byte form.
     *
     * @param index enable-flag index
     * @return wrapper containing the resolved flag, if known
     */
    public static EnableOptional ofNullable(int index) {
        return new EnableOptional(index);
    }

    /**
     * Invoke the action when the index resolves to a known enable flag.
     *
     * @param action action that consumes the resolved flag
     */
    public void ifPresent(Consumer<EnableFlagEnum> action) {
        if (Objects.nonNull(value)) {
            action.accept(value);
        }
    }

    /**
     * Invoke exactly one branch according to whether the index resolves to a flag.
     *
     * @param action      action that consumes the resolved flag
     * @param emptyAction action to run when the index is unknown
     */
    public void ifPresentOrElse(Consumer<EnableFlagEnum> action, Runnable emptyAction) {
        if (Objects.nonNull(value)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

}
