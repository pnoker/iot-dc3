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
package com.serotonin.modbus4j.serial.rtu;

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.base.BaseRequestHandler;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingResponseMessage;

/**
 * <p>RtuRequestHandler class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class RtuRequestHandler extends BaseRequestHandler {
    /**
     * <p>Constructor for RtuRequestHandler.</p>
     *
     * @param slave a {@link ModbusSlaveSet} object.
     */
    public RtuRequestHandler(ModbusSlaveSet slave) {
        super(slave);
    }


    public OutgoingResponseMessage handleRequest(IncomingRequestMessage req) throws Exception {
        RtuMessageRequest rtuRequest = (RtuMessageRequest) req;
        ModbusRequest request = rtuRequest.getModbusRequest();
        ModbusResponse response = handleRequestImpl(request);
        if (response == null)
            return null;
        return new RtuMessageResponse(response);
    }
}
