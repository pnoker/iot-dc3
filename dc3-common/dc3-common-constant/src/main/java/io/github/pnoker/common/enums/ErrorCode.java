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
 * Error response codes returned by the unified API envelope.
 * <p>
 * Each value carries the HTTP status the web layer aligns to, so an error
 * envelope's {@code code} and HTTP status never diverge. Codes that share an
 * HTTP status (e.g. {@link #TOKEN_INVALID} and {@link #IP_INVALID} on 401) keep a
 * distinct remark for diagnostics. {@link #PASSWORD_CHANGE_REQUIRED} and
 * {@link #PASSWORD_EXPIRED} keep distinct codes ({@code R4031}/{@code R4032}) so
 * the login flow can tell the two states apart, even though both map to 403.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ErrorCode implements ResponseCode {

    NOT_FOUND(404, "R404", "Resource does not exist"),
    UNAUTHORIZED(401, "R401", "Unauthorized"),
    TOKEN_INVALID(401, "R401", "Token is invalid"),
    IP_INVALID(401, "R401", "Invalid IP"),
    FORBIDDEN(403, "R403", "Access denied"),
    PASSWORD_CHANGE_REQUIRED(403, "R4031", "Password change required"),
    PASSWORD_EXPIRED(403, "R4032", "Password expired"),
    VALIDATION(422, "R422", "Validation failed"),
    OUT_OF_RANGE(422, "R422", "Number out of range"),
    FAILURE(500, "R500", "Service exception"),
    ;

    @Getter(onMethod_ = {@Override})
    private final int httpStatus;
    @Getter(onMethod_ = {@Override})
    private final String code;
    @Getter(onMethod_ = {@Override})
    private final String remark;

}
