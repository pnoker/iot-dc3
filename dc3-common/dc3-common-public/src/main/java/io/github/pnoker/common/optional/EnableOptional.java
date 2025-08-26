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
 * 自定义 Enable Optional
 *
 * @author pnoker
 * @version 2025.6.0
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
