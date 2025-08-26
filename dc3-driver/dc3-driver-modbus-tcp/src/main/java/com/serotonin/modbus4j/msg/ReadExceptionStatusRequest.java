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
import com.serotonin.modbus4j.code.FunctionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>ReadExceptionStatusRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ReadExceptionStatusRequest extends ModbusRequest {
    /**
     * <p>Constructor for ReadExceptionStatusRequest.</p>
     *
     * @param slaveId a int.
     * @throws ModbusTransportException if any.
     */
    public ReadExceptionStatusRequest(int slaveId) throws ModbusTransportException {
        super(slaveId);
    }

    @Override
    public void validate(Modbus modbus) {
        // no op
    }

    @Override
    protected void writeRequest(ByteQueue queue) {
        // no op
    }

    @Override
    protected void readRequest(ByteQueue queue) {
        // no op
    }

    @Override
    ModbusResponse getResponseInstance(int slaveId) throws ModbusTransportException {
        return new ReadExceptionStatusResponse(slaveId);
    }

    @Override
    ModbusResponse handleImpl(ProcessImage processImage) throws ModbusTransportException {
        return new ReadExceptionStatusResponse(slaveId, processImage.getExceptionStatus());
    }

    @Override
    public byte getFunctionCode() {
        return FunctionCode.READ_EXCEPTION_STATUS;
    }
}
