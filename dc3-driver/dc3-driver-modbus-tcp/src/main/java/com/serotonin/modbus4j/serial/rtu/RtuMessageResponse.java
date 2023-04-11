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
package com.serotonin.modbus4j.serial.rtu;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingResponseMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * Handles the RTU enveloping of modbus responses.
 *
 * @author mlohbihler
 * @version 5.0.0
 */
public class RtuMessageResponse extends RtuMessage implements OutgoingResponseMessage, IncomingResponseMessage {
    static RtuMessageResponse createRtuMessageResponse(ByteQueue queue) throws ModbusTransportException {
        ModbusResponse response = ModbusResponse.createModbusResponse(queue);
        RtuMessageResponse rtuResponse = new RtuMessageResponse(response);

        // Check the CRC
        ModbusUtils.checkCRC(rtuResponse.modbusMessage, queue);

        // Return the data.
        return rtuResponse;
    }

    /**
     * <p>Constructor for RtuMessageResponse.</p>
     *
     * @param modbusResponse a {@link ModbusResponse} object.
     */
    public RtuMessageResponse(ModbusResponse modbusResponse) {
        super(modbusResponse);
    }

    /**
     * <p>getModbusResponse.</p>
     *
     * @return a {@link ModbusResponse} object.
     */
    public ModbusResponse getModbusResponse() {
        return (ModbusResponse) modbusMessage;
    }
}
