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

import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;

/**
 * <p>ReadInputRegistersResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ReadInputRegistersResponse extends ReadResponse {
    ReadInputRegistersResponse(int slaveId, byte[] data) throws ModbusTransportException {
        super(slaveId, data);
    }

    ReadInputRegistersResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getFunctionCode() {
        return FunctionCode.READ_INPUT_REGISTERS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ReadInputRegistersResponse [exceptionCode=" + exceptionCode + ", slaveId=" + slaveId
                + ", getFunctionCode()=" + getFunctionCode() + ", isException()=" + isException()
                + ", getExceptionMessage()=" + getExceptionMessage() + ", getExceptionCode()=" + getExceptionCode()
                + ", toString()=" + super.toString(true) + "]";
    }
}
