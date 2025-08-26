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

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>WriteCoilsResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class WriteCoilsResponse extends ModbusResponse {
    private int startOffset;
    private int numberOfBits;

    WriteCoilsResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    WriteCoilsResponse(int slaveId, int startOffset, int numberOfBits) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        this.numberOfBits = numberOfBits;
    }

    @Override
    public byte getFunctionCode() {
        return FunctionCode.WRITE_COILS;
    }

    @Override
    protected void writeResponse(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfBits);
    }

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
