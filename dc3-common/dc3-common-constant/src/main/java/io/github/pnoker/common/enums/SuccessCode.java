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

package io.github.pnoker.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Success response codes returned by the unified API envelope.
 * <p>
 * Each value carries the HTTP status the web layer aligns to, so a successful
 * envelope's {@code code} and HTTP status never diverge. {@link #DELETE} and
 * {@link #UPDATE} intentionally share {@code R200} with {@link #OK} but keep a
 * distinct remark for log and debug clarity.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum SuccessCode implements ResponseCode {

    OK(200, "R200", "Success"),
    ADD(201, "R201", "Added successfully"),
    DELETE(200, "R200", "Deleted successfully"),
    UPDATE(200, "R200", "Updated successfully"),
    ;

    @Getter(onMethod_ = {@Override})
    private final int httpStatus;
    @Getter(onMethod_ = {@Override})
    private final String code;
    @Getter(onMethod_ = {@Override})
    private final String remark;

}
