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
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ModbusMessage class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class ModbusMessage {
    protected int slaveId;

    /**
     * <p>Constructor for ModbusMessage.</p>
     *
     * @param slaveId a int.
     * @throws ModbusTransportException if any.
     */
    public ModbusMessage(int slaveId) throws ModbusTransportException {
        // Validate the node id. Note that a 0 slave id is a broadcast message.
        if (slaveId < 0 /* || slaveId > 247 */)
            throw new ModbusTransportException("Invalid slave id", slaveId);

        this.slaveId = slaveId;
    }

    /**
     * <p>Getter for the field <code>slaveId</code>.</p>
     *
     * @return a int.
     */
    public int getSlaveId() {
        return slaveId;
    }

    /**
     * <p>getFunctionCode.</p>
     *
     * @return a byte.
     */
    abstract public byte getFunctionCode();

    /**
     * <p>write.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    final public void write(ByteQueue queue) {
        ModbusUtils.pushByte(queue, slaveId);
        writeImpl(queue);
    }

    /**
     * <p>writeImpl.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    abstract protected void writeImpl(ByteQueue queue);

    /**
     * <p>convertToBytes.</p>
     *
     * @param bdata an array of {@link boolean} objects.
     * @return an array of {@link byte} objects.
     */
    protected byte[] convertToBytes(boolean[] bdata) {
        int byteCount = (bdata.length + 7) / 8;
        byte[] data = new byte[byteCount];
        for (int i = 0; i < bdata.length; i++)
            data[i / 8] |= (bdata[i] ? 1 : 0) << (i % 8);
        return data;
    }

    /**
     * <p>convertToBytes.</p>
     *
     * @param sdata an array of {@link short} objects.
     * @return an array of {@link byte} objects.
     */
    protected byte[] convertToBytes(short[] sdata) {
        int byteCount = sdata.length * 2;
        byte[] data = new byte[byteCount];
        for (int i = 0; i < sdata.length; i++) {
            data[i * 2] = (byte) (0xff & (sdata[i] >> 8));
            data[i * 2 + 1] = (byte) (0xff & sdata[i]);
        }
        return data;
    }

    /**
     * <p>convertToBooleans.</p>
     *
     * @param data an array of {@link byte} objects.
     * @return an array of {@link boolean} objects.
     */
    protected boolean[] convertToBooleans(byte[] data) {
        boolean[] bdata = new boolean[data.length * 8];
        for (int i = 0; i < bdata.length; i++)
            bdata[i] = ((data[i / 8] >> (i % 8)) & 0x1) == 1;
        return bdata;
    }

    /**
     * <p>convertToShorts.</p>
     *
     * @param data an array of {@link byte} objects.
     * @return an array of {@link short} objects.
     */
    protected short[] convertToShorts(byte[] data) {
        short[] sdata = new short[data.length / 2];
        for (int i = 0; i < sdata.length; i++)
            sdata[i] = ModbusUtils.toShort(data[i * 2], data[i * 2 + 1]);
        return sdata;
    }
}
