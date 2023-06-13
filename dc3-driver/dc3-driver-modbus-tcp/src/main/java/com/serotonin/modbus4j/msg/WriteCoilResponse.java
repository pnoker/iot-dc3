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
 * <p>WriteCoilResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteCoilResponse extends ModbusResponse {
    private int writeOffset;
    private boolean writeValue;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_COIL;
    }

    WriteCoilResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    WriteCoilResponse(int slaveId, int writeOffset, boolean writeValue) throws ModbusTransportException {
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
        ModbusUtils.pushShort(queue, writeValue ? 0xff00 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        writeOffset = ModbusUtils.popUnsignedShort(queue);
        writeValue = ModbusUtils.popUnsignedShort(queue) == 0xff00;
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
     * <p>isWriteValue.</p>
     *
     * @return a boolean.
     */
    public boolean isWriteValue() {
        return writeValue;
    }
}
