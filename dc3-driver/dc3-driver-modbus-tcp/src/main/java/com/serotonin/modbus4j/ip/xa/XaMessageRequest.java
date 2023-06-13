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
 * @version 5.0.0
 */
public class XaMessageRequest extends XaMessage implements OutgoingRequestMessage, IncomingRequestMessage {
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

    /**
     * <p>Constructor for XaMessageRequest.</p>
     *
     * @param modbusRequest a {@link ModbusRequest} object.
     * @param transactionId a int.
     */
    public XaMessageRequest(ModbusRequest modbusRequest, int transactionId) {
        super(modbusRequest, transactionId);
    }

    /**
     * {@inheritDoc}
     */
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
