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
 * <p>WriteCoilsResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class WriteCoilsResponse extends ModbusResponse {
    private int startOffset;
    private int numberOfBits;

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_COILS;
    }

    WriteCoilsResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    WriteCoilsResponse(int slaveId, int startOffset, int numberOfBits) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        this.numberOfBits = numberOfBits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeResponse(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfBits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        numberOfBits = ModbusUtils.popUnsignedShort(queue);
    }

    /**
     * <p>Getter for the field <code>startOffset</code>.</p>
     *
     * @return a int.
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * <p>Getter for the field <code>numberOfBits</code>.</p>
     *
     * @return a int.
     */
    public int getNumberOfBits() {
        return numberOfBits;
    }
}
