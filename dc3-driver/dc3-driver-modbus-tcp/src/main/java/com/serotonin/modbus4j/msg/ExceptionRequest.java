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

import com.serotonin.modbus4j.Modbus;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.ShouldNeverHappenException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ExceptionRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ExceptionRequest extends ModbusRequest {
    private final byte functionCode;
    private final byte exceptionCode;

    /**
     * <p>Constructor for ExceptionRequest.</p>
     *
     * @param slaveId       a int.
     * @param functionCode  a byte.
     * @param exceptionCode a byte.
     * @throws ModbusTransportException if any.
     */
    public ExceptionRequest(int slaveId, byte functionCode, byte exceptionCode) throws ModbusTransportException {
        super(slaveId);
        this.functionCode = functionCode;
        this.exceptionCode = exceptionCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        throw new ShouldNeverHappenException("wha");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        queue.clear();
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new ExceptionResponse(slaveId, functionCode, exceptionCode);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        return getResponseInstance(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return functionCode;
    }

    /**
     * <p>Getter for the field <code>exceptionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getExceptionCode() {
        return exceptionCode;
    }
}
