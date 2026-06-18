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
 * Raised when a local credential may not be used to log in until its password is changed,
 * either because a password change is mandated or because the password has expired. The
 * carried {@link ErrorCode} lets the web layer return a distinct response code so the
 * client can route the user to the self-service password change flow. Unlike other
 * business exceptions this maps to HTTP 200 (handled in the web layer) because the login
 * flow treats it as a routable outcome rather than a hard failure.
 *
 * @author pnoker
 * @version 2026.6.17
 * @since 2026.6.17
 */
public class PasswordChangeRequiredException extends BusinessException {

    private final ErrorCode errorCode;

    public PasswordChangeRequiredException(ErrorCode errorCode) {
        super(errorCode.getRemark(), null);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
