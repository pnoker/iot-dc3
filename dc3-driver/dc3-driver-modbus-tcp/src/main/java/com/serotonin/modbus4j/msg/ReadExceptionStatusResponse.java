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

import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ReadExceptionStatusResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ReadExceptionStatusResponse extends ModbusResponse {
    private byte exceptionStatus;

    ReadExceptionStatusResponse(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    ReadExceptionStatusResponse(int slaveId, byte exceptionStatus) throws ModbusTransportException {
        super(slaveId);
        this.exceptionStatus = exceptionStatus;
    }


    @Override
    public byte getFunctionCode() {
        return FunctionCode.READ_EXCEPTION_STATUS;
    }


    @Override
    protected void readResponse(ByteQueue queue) {
        exceptionStatus = queue.pop();
    }


    @Override
    protected void writeResponse(ByteQueue queue) {
        queue.push(exceptionStatus);
    }

    /**
     * <p>Getter for the field <code>exceptionStatus</code>.</p>
     *
     * @return a byte.
     */
    public byte getExceptionStatus() {
        return exceptionStatus;
    }
}
