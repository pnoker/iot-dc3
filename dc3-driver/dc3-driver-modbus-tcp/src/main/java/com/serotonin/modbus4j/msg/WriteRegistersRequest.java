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
 * <p>WriteRegistersRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteRegistersRequest extends ModbusRequest {
    private int startOffset;
    private byte[] data;

    /**
     * <p>Constructor for WriteRegistersRequest.</p>
     *
     * @param slaveId     a int.
     * @param startOffset a int.
     * @param sdata       an array of {@link short} objects.
     * @throws ModbusTransportException if any.
     */
    public WriteRegistersRequest(int slaveId, int startOffset, short[] sdata) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        data = convertToBytes(sdata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        int registerCount = data.length / 2;
        if (registerCount < 1 || registerCount > modbus.getMaxWriteRegisterCount())
            throw new ModbusTransportException("Invalid number of registers: " + registerCount, slaveId);
        ModbusUtils.validateEndOffset(startOffset + registerCount - 1);
    }

    WriteRegistersRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, data.length / 2);
        ModbusUtils.pushByte(queue, data.length);
        queue.push(data);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        short[] sdata = convertToShorts(data);
        for (int i = 0; i < sdata.length; i++)
            processImage.writeHoldingRegister(startOffset + i, sdata[i]);
        return new WriteRegistersResponse(slaveId, startOffset, sdata.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_REGISTERS;
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new WriteRegistersResponse(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        ModbusUtils.popUnsignedShort(queue); // register count not needed.
        data = new byte[ModbusUtils.popUnsignedByte(queue)];
        queue.pop(data);
    }
}
