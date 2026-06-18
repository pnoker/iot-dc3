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

/**
 * Common contract for response codes carried by the unified API envelope.
 * <p>
 * Implemented by {@link SuccessCode} and {@link ErrorCode}. The string {@code code}
 * is the stable wire identifier returned to clients; {@code httpStatus} is the HTTP
 * status the web layer aligns to, so the body code and transport status never diverge.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface ResponseCode {

    /**
     * Stable wire code returned to clients, e.g. {@code "R404"}.
     *
     * @return response code
     */
    String getCode();

    /**
     * Default human-readable remark for this code.
     *
     * @return remark
     */
    String getRemark();

    /**
     * HTTP status this code aligns to.
     *
     * @return HTTP status value
     */
    int getHttpStatus();

}
