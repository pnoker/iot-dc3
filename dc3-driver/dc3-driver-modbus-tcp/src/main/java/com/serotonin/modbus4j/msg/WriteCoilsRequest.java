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
 * <p>WriteCoilsRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteCoilsRequest extends ModbusRequest {
    private int startOffset;
    private int numberOfBits;
    private byte[] data;

    /**
     * <p>Constructor for WriteCoilsRequest.</p>
     *
     * @param slaveId     a int.
     * @param startOffset a int.
     * @param bdata       an array of {@link boolean} objects.
     * @throws ModbusTransportException if any.
     */
    public WriteCoilsRequest(int slaveId, int startOffset, boolean[] bdata) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        numberOfBits = bdata.length;
        data = convertToBytes(bdata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        modbus.validateNumberOfBits(numberOfBits);
        ModbusUtils.validateEndOffset(startOffset + numberOfBits - 1);
    }

    WriteCoilsRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfBits);
        ModbusUtils.pushByte(queue, data.length);
        queue.push(data);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        boolean[] bdata = convertToBooleans(data);
        for (int i = 0; i < numberOfBits; i++)
            processImage.writeCoil(startOffset + i, bdata[i]);
        return new WriteCoilsResponse(slaveId, startOffset, numberOfBits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_COILS;
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new WriteCoilsResponse(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        numberOfBits = ModbusUtils.popUnsignedShort(queue);
        data = new byte[ModbusUtils.popUnsignedByte(queue)];
        queue.pop(data);
    }
}
