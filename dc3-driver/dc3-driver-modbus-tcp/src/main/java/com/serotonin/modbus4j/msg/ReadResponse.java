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
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.io.StreamUtils;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ReadResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class ReadResponse extends ModbusResponse {
    private byte[] data;

    ReadResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    ReadResponse(int slaveId, byte[] data) throws ModbusTransportException {
        super(slaveId);
        this.data = data;
    }

    @Override
    protected void readResponse(ByteQueue queue) {
        int numberOfBytes = ModbusUtils.popUnsignedByte(queue);
        if (queue.size() < numberOfBytes)
            throw new ArrayIndexOutOfBoundsException();

        data = new byte[numberOfBytes];
        queue.pop(data);
    }

    @Override
    protected void writeResponse(ByteQueue queue) {
        ModbusUtils.pushByte(queue, data.length);
        queue.push(data);
    }

    /**
     * <p>Getter for the field <code>data</code>.</p>
     *
     * @return an array of {@link byte} objects.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * <p>getShortData.</p>
     *
     * @return an array of {@link short} objects.
     */
    public short[] getShortData() {
        return convertToShorts(data);
    }

    /**
     * <p>getBooleanData.</p>
     *
     * @return an array of {@link boolean} objects.
     */
    public boolean[] getBooleanData() {
        return convertToBooleans(data);
    }

    /**
     * <p>toString.</p>
     *
     * @param numeric a boolean.
     * @return a {@link String} object.
     */
    public String toString(boolean numeric) {
        if (data == null)
            return "ReadResponse [null]";
        return "ReadResponse [len=" + (numeric ? data.length / 2 : data.length * 8) + ", " + StreamUtils.dumpHex(data)
                + "]";
    }
}
