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

import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ExceptionResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ExceptionResponse extends ModbusResponse {
    private final byte functionCode;

    /**
     * <p>Constructor for ExceptionResponse.</p>
     *
     * @param slaveId       a int.
     * @param functionCode  a byte.
     * @param exceptionCode a byte.
     * @throws ModbusTransportException if any.
     */
    public ExceptionResponse(int slaveId, byte functionCode, byte exceptionCode) throws ModbusTransportException {
        super(slaveId);
        this.functionCode = functionCode;
        setException(exceptionCode);
    }

    @Override
    public byte getFunctionCode() {
        return functionCode;
    }

    @Override
    protected void readResponse(ByteQueue queue) {
        // no op
    }

    @Override
    protected void writeResponse(ByteQueue queue) {
        // no op
    }
}
