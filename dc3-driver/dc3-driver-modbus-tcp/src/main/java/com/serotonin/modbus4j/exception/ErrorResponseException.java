/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.serotonin.modbus4j.exception;

import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;

/**
 * <p>ErrorResponseException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return errorResponse.getExceptionMessage();
    }
}
