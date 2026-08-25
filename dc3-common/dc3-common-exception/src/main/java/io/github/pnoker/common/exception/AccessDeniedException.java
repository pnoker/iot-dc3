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

package io.github.pnoker.common.exception;

import io.github.pnoker.common.enums.ErrorCode;

/**
 * Thrown when an authenticated user lacks the required permission for an operation.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public class AccessDeniedException extends ServiceException {

    /**
     * Exception with a plain failure detail.
     *
     * @param message failure detail
     */
    public AccessDeniedException(String message) {
        super(message);
    }

    /**
     * Exception with a failure detail and cause.
     *
     * @param message failure detail or underlying failure
     * @param cause   failure detail or underlying failure
     */
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.FORBIDDEN;
    }
}
