/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;

/**
 * <p>ReadDiscreteInputsRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2024.3.9
 */
public class ReadDiscreteInputsRequest extends ReadBinaryRequest {
    /**
     * <p>Constructor for ReadDiscreteInputsRequest.</p>
     *
     * @param slaveId      a int.
     * @param startOffset  a int.
     * @param numberOfBits a int.
     * @throws ModbusTransportException if any.
     */
    public ReadDiscreteInputsRequest(int slaveId, int startOffset, int numberOfBits) throws ModbusTransportException {
        super(slaveId, startOffset, numberOfBits);
    }

    ReadDiscreteInputsRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    @Override
    public byte getFunctionCode() {
        return FunctionCode.READ_DISCRETE_INPUTS;
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        return new ReadDiscreteInputsResponse(slaveId, getData(processImage));
    }

    @Override
    protected boolean getBinary(ProcessImage processImage, int index) throws ModbusTransportException {
        return processImage.getInput(index);
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new ReadDiscreteInputsResponse(slaveId);
    }
}
