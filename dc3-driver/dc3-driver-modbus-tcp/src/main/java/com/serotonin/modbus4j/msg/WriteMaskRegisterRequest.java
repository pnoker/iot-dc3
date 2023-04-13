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
import com.serotonin.modbus4j.exception.ModbusIdException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>WriteMaskRegisterRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteMaskRegisterRequest extends ModbusRequest {
    private int writeOffset;

    /**
     * The andMask determines which bits we want to change. If a bit in the andMask is 1, it indicates that the value
     * should not be changed. If it is zero, it should be changed according to the orMask value for that bit.
     */
    private int andMask;

    /**
     * The orMask determines what value a bit will have after writing if the andMask allows that bit to be changed. If a
     * changable bit in the orMask is 0, the bit in the result will be zero. Ditto for 1.
     */
    private int orMask;

    /**
     * Constructor that defaults the masks to have no effect on the register. Use the setBit function to modify mask
     * values.
     *
     * @param slaveId     a int.
     * @param writeOffset a int.
     * @throws ModbusTransportException when necessary
     */
    public WriteMaskRegisterRequest(int slaveId, int writeOffset) throws ModbusTransportException {
        this(slaveId, writeOffset, 0xffff, 0);
    }

    /**
     * <p>Constructor for WriteMaskRegisterRequest.</p>
     *
     * @param slaveId     a int.
     * @param writeOffset a int.
     * @param andMask     a int.
     * @param orMask      a int.
     * @throws ModbusTransportException if any.
     */
    public WriteMaskRegisterRequest(int slaveId, int writeOffset, int andMask, int orMask)
            throws ModbusTransportException {
        super(slaveId);
        this.writeOffset = writeOffset;
        this.andMask = andMask;
        this.orMask = orMask;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(writeOffset);
    }

    /**
     * <p>setBit.</p>
     *
     * @param bit   a int.
     * @param value a boolean.
     */
    public void setBit(int bit, boolean value) {
        if (bit < 0 || bit > 15)
            throw new ModbusIdException("Bit must be between 0 and 15 inclusive");

        // Set the bit in the andMask to 0 to allow writing.
        andMask = andMask & ~(1 << bit);

        // Set the bit in the orMask to write the correct value.
        if (value)
            orMask = orMask | 1 << bit;
        else
            orMask = orMask & ~(1 << bit);
    }

    WriteMaskRegisterRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, writeOffset);
        ModbusUtils.pushShort(queue, andMask);
        ModbusUtils.pushShort(queue, orMask);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        short value = processImage.getHoldingRegister(writeOffset);
        value = (short) ((value & andMask) | (orMask & (~andMask)));
        processImage.writeHoldingRegister(writeOffset, value);
        return new WriteMaskRegisterResponse(slaveId, writeOffset, andMask, orMask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_MASK_REGISTER;
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new WriteMaskRegisterResponse(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        writeOffset = ModbusUtils.popUnsignedShort(queue);
        andMask = ModbusUtils.popUnsignedShort(queue);
        orMask = ModbusUtils.popUnsignedShort(queue);
    }
}
