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
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.IllegalFunctionException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.exception.SlaveIdNotEqual;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract ModbusResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ModbusResponse extends ModbusMessage {
    /**
     * Constant <code>MAX_FUNCTION_CODE=(byte) 0x80</code>
     */
    protected static final byte MAX_FUNCTION_CODE = (byte) 0x80;

    /**
     * <p>createModbusResponse.</p>
     *
     * @param queue a {@link ByteQueue} object.
     * @return a {@link ModbusResponse} object.
     * @throws ModbusTransportException if any.
     */
    public static ModbusResponse createModbusResponse(ByteQueue queue) throws ModbusTransportException {
        int slaveId = ModbusUtils.popUnsignedByte(queue);
        byte functionCode = queue.pop();
        boolean isException = false;

        if (greaterThan(functionCode, MAX_FUNCTION_CODE)) {
            isException = true;
            functionCode -= MAX_FUNCTION_CODE;
        }

        ModbusResponse response = null;
        if (functionCode == FunctionCode.READ_COILS)
            response = new ReadCoilsResponse(slaveId);
        else if (functionCode == FunctionCode.READ_DISCRETE_INPUTS)
            response = new ReadDiscreteInputsResponse(slaveId);
        else if (functionCode == FunctionCode.READ_HOLDING_REGISTERS)
            response = new ReadHoldingRegistersResponse(slaveId);
        else if (functionCode == FunctionCode.READ_INPUT_REGISTERS)
            response = new ReadInputRegistersResponse(slaveId);
        else if (functionCode == FunctionCode.WRITE_COIL)
            response = new WriteCoilResponse(slaveId);
        else if (functionCode == FunctionCode.WRITE_REGISTER)
            response = new WriteRegisterResponse(slaveId);
        else if (functionCode == FunctionCode.READ_EXCEPTION_STATUS)
            response = new ReadExceptionStatusResponse(slaveId);
        else if (functionCode == FunctionCode.WRITE_COILS)
            response = new WriteCoilsResponse(slaveId);
        else if (functionCode == FunctionCode.WRITE_REGISTERS)
            response = new WriteRegistersResponse(slaveId);
        else if (functionCode == FunctionCode.REPORT_SLAVE_ID)
            response = new ReportSlaveIdResponse(slaveId);
        else if (functionCode == FunctionCode.WRITE_MASK_REGISTER)
            response = new WriteMaskRegisterResponse(slaveId);
        else
            throw new IllegalFunctionException(functionCode, slaveId);

        response.read(queue, isException);

        return response;
    }

    protected byte exceptionCode = -1;

    ModbusResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * <p>isException.</p>
     *
     * @return a boolean.
     */
    public boolean isException() {
        return exceptionCode != -1;
    }

    /**
     * <p>getExceptionMessage.</p>
     *
     * @return a {@link String} object.
     */
    public String getExceptionMessage() {
        return ExceptionCode.getExceptionMessage(exceptionCode);
    }

    void setException(byte exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    /**
     * <p>Getter for the field <code>exceptionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getExceptionCode() {
        return exceptionCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final protected void writeImpl(ByteQueue queue) {
        if (isException()) {
            queue.push((byte) (getFunctionCode() + MAX_FUNCTION_CODE));
            queue.push(exceptionCode);
        } else {
            queue.push(getFunctionCode());
            writeResponse(queue);
        }
    }

    /**
     * <p>writeResponse.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    abstract protected void writeResponse(ByteQueue queue);

    void read(ByteQueue queue, boolean isException) {
        if (isException)
            exceptionCode = queue.pop();
        else
            readResponse(queue);
    }

    /**
     * <p>readResponse.</p>
     *
     * @param queue a {@link ByteQueue} object.
     */
    abstract protected void readResponse(ByteQueue queue);

    private static boolean greaterThan(byte b1, byte b2) {
        int i1 = b1 & 0xff;
        int i2 = b2 & 0xff;
        return i1 > i2;
    }

    /**
     * Ensure that the Response slave id is equal to the requested slave id
     *
     * @param request
     * @throws ModbusTransportException
     */
    public void validateResponse(ModbusRequest request) throws ModbusTransportException {
        if (getSlaveId() != request.slaveId)
            throw new SlaveIdNotEqual(request.slaveId, getSlaveId());
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     * @throws Exception if any.
     */
    public static void main(String[] args) throws Exception {
        ByteQueue queue = new ByteQueue(new byte[]{3, 2});
        ModbusResponse r = createModbusResponse(queue);
        System.out.println(r);
    }
}
