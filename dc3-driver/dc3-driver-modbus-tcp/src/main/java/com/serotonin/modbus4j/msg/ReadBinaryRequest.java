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
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ReadBinaryRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        modbus.validateNumberOfBits(numberOfBits);
        ModbusUtils.validateEndOffset(startOffset + numberOfBits - 1);
    }

    ReadBinaryRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfBits);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ReadBinaryRequest [startOffset=" + startOffset + ", numberOfBits=" + numberOfBits + "]";
    }
}
