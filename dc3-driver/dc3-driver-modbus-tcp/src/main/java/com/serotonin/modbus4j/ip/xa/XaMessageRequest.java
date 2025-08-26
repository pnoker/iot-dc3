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
package com.serotonin.modbus4j.ip.xa;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.sero.messaging.IncomingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>XaMessageRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class XaMessageRequest extends XaMessage implements OutgoingRequestMessage, IncomingRequestMessage {
    /**
     * <p>Constructor for XaMessageRequest.</p>
     *
     * @param modbusRequest a {@link ModbusRequest} object.
     * @param transactionId a int.
     */
    public XaMessageRequest(ModbusRequest modbusRequest, int transactionId) {
        super(modbusRequest, transactionId);
    }

    static XaMessageRequest createXaMessageRequest(ByteQueue queue) throws ModbusTransportException {
        // Remove the XA header
        int transactionId = ModbusUtils.popShort(queue);
        int protocolId = ModbusUtils.popShort(queue);
        if (protocolId != ModbusUtils.IP_PROTOCOL_ID)
            throw new ModbusTransportException("Unsupported IP protocol id: " + protocolId);
        ModbusUtils.popShort(queue); // Length, which we don't care about.

        // Create the modbus response.
        ModbusRequest request = ModbusRequest.createModbusRequest(queue);
        return new XaMessageRequest(request, transactionId);
    }

    @Override
    public boolean expectsResponse() {
        return modbusMessage.getSlaveId() != 0;
    }

    /**
     * <p>getModbusRequest.</p>
     *
     * @return a {@link ModbusRequest} object.
     */
    public ModbusRequest getModbusRequest() {
        return (ModbusRequest) modbusMessage;
    }
}
