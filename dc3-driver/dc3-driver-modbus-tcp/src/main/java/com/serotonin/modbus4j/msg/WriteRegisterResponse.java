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
 * <p>WriteRegisterResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteRegisterResponse extends ModbusResponse {
    private int writeOffset;
    private int writeValue;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_REGISTER;
    }

    WriteRegisterResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    WriteRegisterResponse(int slaveId, int writeOffset, int writeValue) throws ModbusTransportException {
        super(slaveId);
        this.writeOffset = writeOffset;
        this.writeValue = writeValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeResponse(ByteQueue queue) {
        ModbusUtils.pushShort(queue, writeOffset);
        ModbusUtils.pushShort(queue, writeValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        writeOffset = ModbusUtils.popUnsignedShort(queue);
        writeValue = ModbusUtils.popUnsignedShort(queue);
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
     * <p>Getter for the field <code>writeValue</code>.</p>
     *
     * @return a int.
     */
    public int getWriteValue() {
        return writeValue;
    }
}
