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
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ReadBinaryRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class ReadBinaryRequest extends ModbusRequest {
    private int startOffset;
    private int numberOfBits;

    /**
     * <p>Constructor for ReadBinaryRequest.</p>
     *
     * @param slaveId      a int.
     * @param startOffset  a int.
     * @param numberOfBits a int.
     * @throws ModbusTransportException if any.
     */
    public ReadBinaryRequest(int slaveId, int startOffset, int numberOfBits) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        this.numberOfBits = numberOfBits;
    }

    ReadBinaryRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        modbus.validateNumberOfBits(numberOfBits);
        ModbusUtils.validateEndOffset(startOffset + numberOfBits - 1);
    }

    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfBits);
    }

    @Override
    protected void readRequest(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        numberOfBits = ModbusUtils.popUnsignedShort(queue);
    }

    /**
     * <p>getData.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @return an array of {@link byte} objects.
     * @throws ModbusTransportException if any.
     */
    protected byte[] getData(ProcessImage processImage) throws ModbusTransportException {
        boolean[] data = new boolean[numberOfBits];

        // Get the data from the process image.
        for (int i = 0; i < numberOfBits; i++)
            data[i] = getBinary(processImage, i + startOffset);

        // Convert the boolean array into an array of bytes.
        return convertToBytes(data);
    }

    /**
     * <p>getBinary.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @param index        a int.
     * @return a boolean.
     * @throws ModbusTransportException if any.
     */
    abstract protected boolean getBinary(ProcessImage processImage, int index) throws ModbusTransportException;

    @Override
    public String toString() {
        return "ReadBinaryRequest [startOffset=" + startOffset + ", numberOfBits=" + numberOfBits + "]";
    }
}
