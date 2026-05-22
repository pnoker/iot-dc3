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

package io.github.pnoker.common.data.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates point command parameters including value-range constraints defined
 * in {@code point_ext.constraints} (min, max, enum, step).
 * <p>
 * Currently performs type-level validation that the value is non-blank for writes.
 * Extended constraint validation from {@code point_ext} can be added when the
 * {@code constraints} field is populated in the point schema.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Component
public class PointCommandValidator {

    /**
     * Validate a write value against the point's constraints.
     *
     * @param value    raw value string to validate
     * @param pointExt point extension JSON (may contain constraints for future use)
     * @throws IllegalArgumentException if the value fails validation
     */
    public void validateWriteValue(String value, String pointExt) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Write value must not be blank");
        }
    }
}
