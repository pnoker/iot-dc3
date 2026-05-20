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

/**
 * Exception for resource not found errors.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException() {
        this(null);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    public NotFoundException(String template, Object... params) {
        super(ExceptionMessageFormatter.format(template, params), ExceptionMessageFormatter.cause(params));
    }

}
