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
 * @version 5.0.0
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
