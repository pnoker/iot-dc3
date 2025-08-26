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
package io.github.pnoker.driver.exception;

/**
 * The Class S7Exception is an exception related to S7 Communication
 *
 * @author Thomas Rudin
 */
public final class S7Exception extends RuntimeException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new s7 exception.
     */
    public S7Exception() {
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param message the message
     */
    public S7Exception(final String message) {
        super(message);
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public S7Exception(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new s7 exception.
     *
     * @param cause the cause
     */
    public S7Exception(final Throwable cause) {
        super(cause);
    }

}
