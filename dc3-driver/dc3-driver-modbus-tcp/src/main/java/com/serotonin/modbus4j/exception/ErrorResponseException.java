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
package com.serotonin.modbus4j.exception;

import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;

/**
 * <p>ErrorResponseException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ErrorResponseException extends Exception {
    private static final long serialVersionUID = -1;

    private final ModbusRequest originalRequest;
    private final ModbusResponse errorResponse;

    /**
     * <p>Constructor for ErrorResponseException.</p>
     *
     * @param originalRequest a {@link ModbusRequest} object.
     * @param errorResponse   a {@link ModbusResponse} object.
     */
    public ErrorResponseException(ModbusRequest originalRequest, ModbusResponse errorResponse) {
        this.originalRequest = originalRequest;
        this.errorResponse = errorResponse;
    }

    /**
     * <p>Getter for the field <code>errorResponse</code>.</p>
     *
     * @return a {@link ModbusResponse} object.
     */
    public ModbusResponse getErrorResponse() {
        return errorResponse;
    }

    /**
     * <p>Getter for the field <code>originalRequest</code>.</p>
     *
     * @return a {@link ModbusRequest} object.
     */
    public ModbusRequest getOriginalRequest() {
        return originalRequest;
    }

    @Override
    public String getMessage() {
        return errorResponse.getExceptionMessage();
    }
}
