/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
package com.serotonin.modbus4j.ip.encap;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.sero.messaging.IncomingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>EncapMessageRequest class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class EncapMessageRequest extends EncapMessage implements OutgoingRequestMessage, IncomingRequestMessage {
    /**
     * <p>Constructor for EncapMessageRequest.</p>
     *
     * @param modbusRequest a {@link ModbusRequest} object.
     */
    public EncapMessageRequest(ModbusRequest modbusRequest) {
        super(modbusRequest);
    }

    static EncapMessageRequest createEncapMessageRequest(ByteQueue queue) throws ModbusTransportException {
        // Create the modbus response.
        ModbusRequest request = ModbusRequest.createModbusRequest(queue);
        EncapMessageRequest encapRequest = new EncapMessageRequest(request);

        // Check the CRC
        ModbusUtils.checkCRC(encapRequest.modbusMessage, queue);

        return encapRequest;
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
