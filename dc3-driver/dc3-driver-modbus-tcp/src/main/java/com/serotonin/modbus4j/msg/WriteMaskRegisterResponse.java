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

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>WriteMaskRegisterResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteMaskRegisterResponse extends ModbusResponse {
    private int writeOffset;
    private int andMask;
    private int orMask;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_MASK_REGISTER;
    }

    WriteMaskRegisterResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    WriteMaskRegisterResponse(int slaveId, int writeOffset, int andMask, int orMask) throws ModbusTransportException {
        super(slaveId);
        this.writeOffset = writeOffset;
        this.andMask = andMask;
        this.orMask = orMask;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeResponse(ByteQueue queue) {
        ModbusUtils.pushShort(queue, writeOffset);
        ModbusUtils.pushShort(queue, andMask);
        ModbusUtils.pushShort(queue, orMask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        writeOffset = ModbusUtils.popUnsignedShort(queue);
        andMask = ModbusUtils.popUnsignedShort(queue);
        orMask = ModbusUtils.popUnsignedShort(queue);
    }

    /**
     * <p>Getter for the field <code>writeOffset</code>.</p>
     *
     * @return a int.
     */
    public int getWriteOffset() {
        return writeOffset;
    }

    /**
     * <p>Getter for the field <code>andMask</code>.</p>
     *
     * @return a int.
     */
    public int getAndMask() {
        return andMask;
    }

    /**
     * <p>Getter for the field <code>orMask</code>.</p>
     *
     * @return a int.
     */
    public int getOrMask() {
        return orMask;
    }
}
