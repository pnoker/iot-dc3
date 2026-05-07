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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Condition;

/**
 * MapStruct condition methods for null-safe mapping.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class MapStructUtil {

    private MapStructUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    @Condition
    public static boolean isNotEmpty(String value) {
        return StringUtils.isNotEmpty(value);
    }

    @Condition
    public static boolean isValidNumber(Number value) {
        return switch (value) {
            case null -> false;
            case Double d -> !Double.isNaN(d) && !Double.isInfinite(d);
            case Float f -> !Float.isNaN(f) && !Float.isInfinite(f);
            default -> true;
        };
    }

}
