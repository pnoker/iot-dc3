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
 * <p>Abstract ReadNumericRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ReadNumericRequest extends ModbusRequest {
    private int startOffset;
    private int numberOfRegisters;

    /**
     * <p>Constructor for ReadNumericRequest.</p>
     *
     * @param slaveId           a int.
     * @param startOffset       a int.
     * @param numberOfRegisters a int.
     * @throws ModbusTransportException if any.
     */
    public ReadNumericRequest(int slaveId, int startOffset, int numberOfRegisters) throws ModbusTransportException {
        super(slaveId);
        this.startOffset = startOffset;
        this.numberOfRegisters = numberOfRegisters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Modbus modbus) throws ModbusTransportException {
        ModbusUtils.validateOffset(startOffset);
        modbus.validateNumberOfRegisters(numberOfRegisters);
        ModbusUtils.validateEndOffset(startOffset + numberOfRegisters - 1);
    }

    ReadNumericRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRequest(ByteQueue queue) {
        ModbusUtils.pushShort(queue, startOffset);
        ModbusUtils.pushShort(queue, numberOfRegisters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readRequest(ByteQueue queue) {
        startOffset = ModbusUtils.popUnsignedShort(queue);
        numberOfRegisters = ModbusUtils.popUnsignedShort(queue);
    }

    /**
     * <p>getData.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @return an array of {@link byte} objects.
     * @throws ModbusTransportException if any.
     */
    protected byte[] getData(ProcessImage processImage) throws ModbusTransportException {
        short[] data = new short[numberOfRegisters];

        // Get the data from the process image.
        for (int i = 0; i < numberOfRegisters; i++)
            data[i] = getNumeric(processImage, i + startOffset);

        return convertToBytes(data);
    }

    /**
     * <p>getNumeric.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @param index        a int.
     * @return a short.
     * @throws ModbusTransportException if any.
     */
    abstract protected short getNumeric(ProcessImage processImage, int index) throws ModbusTransportException;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ReadNumericRequest [startOffset=" + startOffset + ", numberOfRegisters=" + numberOfRegisters + "]";
    }
}
