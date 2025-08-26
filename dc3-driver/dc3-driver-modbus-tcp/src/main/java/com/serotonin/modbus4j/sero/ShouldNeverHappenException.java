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
package com.serotonin.modbus4j.sero;

/**
 * <p>ShouldNeverHappenException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ShouldNeverHappenException extends RuntimeException {
    private static final long serialVersionUID = -1;

    /**
     * <p>Constructor for ShouldNeverHappenException.</p>
     *
     * @param message a {@link String} object.
     */
    public ShouldNeverHappenException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for ShouldNeverHappenException.</p>
     *
     * @param cause a {@link Throwable} object.
     */
    public ShouldNeverHappenException(Throwable cause) {
        super(cause);
    }
}
