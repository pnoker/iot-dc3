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
package com.serotonin.modbus4j.serial.ascii;

import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingResponseMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>AsciiMessageResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class AsciiMessageResponse extends AsciiMessage implements OutgoingResponseMessage, IncomingResponseMessage {
    static AsciiMessageResponse createAsciiMessageResponse(ByteQueue queue) throws ModbusTransportException {
        ByteQueue msgQueue = getUnasciiMessage(queue);
        ModbusResponse response = ModbusResponse.createModbusResponse(msgQueue);
        AsciiMessageResponse asciiResponse = new AsciiMessageResponse(response);

        // Return the data.
        return asciiResponse;
    }

    /**
     * <p>Constructor for AsciiMessageResponse.</p>
     *
     * @param modbusMessage a {@link ModbusMessage} object.
     */
    public AsciiMessageResponse(ModbusMessage modbusMessage) {
        super(modbusMessage);
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
