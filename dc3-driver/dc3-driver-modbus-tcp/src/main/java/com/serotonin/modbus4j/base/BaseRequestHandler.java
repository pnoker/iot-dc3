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
package com.serotonin.modbus4j.base;

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.RequestHandler;

/**
 * <p>Abstract BaseRequestHandler class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class BaseRequestHandler implements RequestHandler {
    protected ModbusSlaveSet slave;

    /**
     * <p>Constructor for BaseRequestHandler.</p>
     *
     * @param slave a {@link ModbusSlaveSet} object.
     */
    public BaseRequestHandler(ModbusSlaveSet slave) {
        this.slave = slave;
    }

    /**
     * <p>handleRequestImpl.</p>
     *
     * @param request a {@link ModbusRequest} object.
     * @return a {@link ModbusResponse} object.
     * @throws ModbusTransportException if any.
     */
    protected ModbusResponse handleRequestImpl(ModbusRequest request) throws ModbusTransportException {
        request.validate(slave);

        int slaveId = request.getSlaveId();

        // Check the slave id.
        if (slaveId == 0) {
            // Broadcast message. Send to all process images.
            for (ProcessImage processImage : slave.getProcessImages())
                request.handle(processImage);
            return null;
        }

        // Find the process image to which to send.
        ProcessImage processImage = slave.getProcessImage(slaveId);
        if (processImage == null)
            return null;

        return request.handle(processImage);
    }
}
