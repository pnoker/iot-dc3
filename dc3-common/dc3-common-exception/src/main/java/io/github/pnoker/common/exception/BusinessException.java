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
 * Base class for all business exceptions.
 * <p>
 * Each concrete subclass binds to a fixed {@link ErrorCode} by overriding
 * {@link #getErrorCode()}. The web layer reads that code to align the response
 * body code and HTTP status, so a thrown exception alone determines both —
 * failure semantics no longer collapse to a generic {@code R500}.
 * <p>
 * Subclasses keep the project's standard three-constructor shape (no-arg, cause,
 * template+params); this base provides the matching {@code super(...)} plumbing.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException() {
        super();
    }

    protected BusinessException(Throwable cause) {
        super(cause);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The fixed error code this exception type maps to.
     *
     * @return {@link ErrorCode}
     */
    public abstract ErrorCode getErrorCode();

}
