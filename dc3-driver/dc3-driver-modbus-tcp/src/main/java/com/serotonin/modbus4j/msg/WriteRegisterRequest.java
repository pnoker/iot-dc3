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
import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>WriteRegisterRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteRegisterRequest extends ModbusRequest {
    private int writeOffset;
    private int writeValue;

    /**
     * <p>Constructor for WriteRegisterRequest.</p>
     *
     * @param slaveId     a int.
     * @param writeOffset a int.
     * @param writeValue  a int.
     * @throws ModbusTransportException if any.
     */
    public WriteRegisterRequest(int slaveId, int writeOffset, int writeValue) throws ModbusTransportException {
        super(slaveId);
        this.writeOffset = writeOffset;
        this.writeValue = writeValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(writeOffset);
    }

    WriteRegisterRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, writeOffset);
        ModbusUtils.pushShort(queue, writeValue);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        processImage.writeHoldingRegister(writeOffset, (short) writeValue);
        return new WriteRegisterResponse(slaveId, writeOffset, writeValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_REGISTER;
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new WriteRegisterResponse(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        writeOffset = ModbusUtils.popUnsignedShort(queue);
        writeValue = ModbusUtils.popUnsignedShort(queue);
    }
}
