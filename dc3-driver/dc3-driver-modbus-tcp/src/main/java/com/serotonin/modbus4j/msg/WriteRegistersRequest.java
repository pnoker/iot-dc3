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
 * @version 2025.6.0
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

    WriteRegistersRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        int registerCount = data.length / 2;
        if (registerCount < 1 || registerCount > modbus.getMaxWriteRegisterCount())
            throw new ModbusTransportException("Invalid number of registers: " + registerCount, slaveId);
        ModbusUtils.validateEndOffset(startOffset + registerCount - 1);
    }

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

    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_REGISTERS;
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new WriteRegistersResponse(slaveId);
    }

    @Override
    protected void readRequest(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        ModbusUtils.popUnsignedShort(queue); // register count not needed.
        data = new byte[ModbusUtils.popUnsignedByte(queue)];
        queue.pop(data);
    }
}
