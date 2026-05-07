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

import io.github.pnoker.common.constant.common.DefaultConstant;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Custom Integer Optional Class
 * <p>
 * Optional wrapper class for integer operations. Provides utility methods for null safety
 * and empty checks with consumer-based conditional operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public final class IntegerOptional {

    private final Integer value;

    private IntegerOptional(Integer value) {
        this.value = value;
    }

    public static IntegerOptional ofNullable(Integer value) {
        return new IntegerOptional(value);
    }

    public void ifPresent(IntConsumer action) {
        if (Objects.nonNull(value) && value > DefaultConstant.ZERO) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(IntConsumer action, Runnable emptyAction) {
        if (Objects.nonNull(value) && value > DefaultConstant.ZERO) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

}
