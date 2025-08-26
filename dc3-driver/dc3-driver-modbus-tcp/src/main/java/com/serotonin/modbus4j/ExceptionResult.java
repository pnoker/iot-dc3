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
package com.serotonin.modbus4j;

import com.serotonin.modbus4j.code.ExceptionCode;

/**
 * <p>ExceptionResult class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ExceptionResult {
    private final byte exceptionCode;
    private final String exceptionMessage;

    /**
     * <p>Constructor for ExceptionResult.</p>
     *
     * @param exceptionCode a byte.
     */
    public ExceptionResult(byte exceptionCode) {
        this.exceptionCode = exceptionCode;
        exceptionMessage = ExceptionCode.getExceptionMessage(exceptionCode);
    }

    /**
     * <p>Getter for the field <code>exceptionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getExceptionCode() {
        return exceptionCode;
    }

    /**
     * <p>Getter for the field <code>exceptionMessage</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
