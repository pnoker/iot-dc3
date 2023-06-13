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
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.io.StreamUtils;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ReadResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readResponse(ByteQueue queue) {
        int numberOfBytes = ModbusUtils.popUnsignedByte(queue);
        if (queue.size() < numberOfBytes)
            throw new ArrayIndexOutOfBoundsException();

        data = new byte[numberOfBytes];
        queue.pop(data);
    }

    /**
     * {@inheritDoc}
     */
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
