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
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ModbusRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ModbusRequest extends ModbusMessage {
    /**
     * <p>createModbusRequest.</p>
     *
     * @param queue a {@link ByteQueue} object.
     * @return a {@link ModbusRequest} object.
     * @throws ModbusTransportException if any.
     */
    public static ModbusRequest createModbusRequest(ByteQueue queue) throws ModbusTransportException {
        int slaveId = ModbusUtils.popUnsignedByte(queue);
        byte functionCode = queue.pop();

        ModbusRequest request = null;
        if (functionCode == FunctionCode.READ_COILS)
            request = new ReadCoilsRequest(slaveId);
        else if (functionCode == FunctionCode.READ_DISCRETE_INPUTS)
            request = new ReadDiscreteInputsRequest(slaveId);
        else if (functionCode == FunctionCode.READ_HOLDING_REGISTERS)
            request = new ReadHoldingRegistersRequest(slaveId);
        else if (functionCode == FunctionCode.READ_INPUT_REGISTERS)
            request = new ReadInputRegistersRequest(slaveId);
        else if (functionCode == FunctionCode.WRITE_COIL)
            request = new WriteCoilRequest(slaveId);
        else if (functionCode == FunctionCode.WRITE_REGISTER)
            request = new WriteRegisterRequest(slaveId);
        else if (functionCode == FunctionCode.READ_EXCEPTION_STATUS)
            request = new ReadExceptionStatusRequest(slaveId);
        else if (functionCode == FunctionCode.WRITE_COILS)
            request = new WriteCoilsRequest(slaveId);
        else if (functionCode == FunctionCode.WRITE_REGISTERS)
            request = new WriteRegistersRequest(slaveId);
        else if (functionCode == FunctionCode.REPORT_SLAVE_ID)
            request = new ReportSlaveIdRequest(slaveId);
            // else if (functionCode == FunctionCode.WRITE_MASK_REGISTER)
            // request = new WriteMaskRegisterRequest(slaveId);
        else
            request = new ExceptionRequest(slaveId, functionCode, ExceptionCode.ILLEGAL_FUNCTION);

        request.readRequest(queue);

        return request;
    }

    ModbusRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * <p>validate.</p>
     *
     * @param modbus a {@link Modbus} object.
     * @throws ModbusTransportException if any.
     */
    abstract public void validate(Modbus modbus) throws ModbusTransportException;

    /**
     * <p>handle.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @return a {@link ModbusResponse} object.
     * @throws ModbusTransportException if any.
     */
    public ModbusResponse handle(ProcessImage processImage) throws ModbusTransportException {
        try {
            try {
                return handleImpl(processImage);
            } catch (IllegalDataAddressException e) {
                return handleException(ExceptionCode.ILLEGAL_DATA_ADDRESS);
            }
        } catch (Exception e) {
            return handleException(ExceptionCode.SLAVE_DEVICE_FAILURE);
        }
    }

    abstract ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException;

    /**
     * <p>readRequest.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    abstract protected void readRequest(ByteQueue queue);

    ModbusResponse handleException(byte exceptionCode) throws ModbusTransportException {
        ModbusResponse response = getResponseInstance(slaveId);
        response.setException(exceptionCode);
        return response;
    }

    abstract ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException;

    /**
     * {@inheritDoc}
     */
    @Override
    final protected void writeImpl(ByteQueue queue) {
        queue.push(getFunctionCode());
        writeRequest(queue);
    }

    /**
     * <p>writeRequest.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    abstract protected void writeRequest(ByteQueue queue);
}
