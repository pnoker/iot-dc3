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
package com.serotonin.modbus4j.msg;

import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ExceptionResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ExceptionResponse extends ModbusResponse {
    private final byte functionCode;

    /**
     * <p>Constructor for ExceptionResponse.</p>
     *
     * @param slaveId       a int.
     * @param functionCode  a byte.
     * @param exceptionCode a byte.
     * @throws ModbusTransportException if any.
     */
    public ExceptionResponse(int slaveId, byte functionCode, byte exceptionCode) throws ModbusTransportException {
        super(slaveId);
        this.functionCode = functionCode;
        setException(exceptionCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return functionCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeResponse(ByteQueue queue) {
        // no op
    }
}
