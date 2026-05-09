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

package io.github.pnoker.common.facade.entity.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Transport-neutral page envelope for facade return values.
 * <p>
 * Intentionally does not extend MyBatis-Plus {@code Page} so that
 * {@code dc3-common-facade-api} keeps zero persistence dependencies.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FacadePage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long current;

    private long size;

    private long total;

    private long pages;

    private List<T> records;

    public static <T> FacadePage<T> empty() {
        return new FacadePage<>(1L, 0L, 0L, 0L, Collections.emptyList());
    }

}
