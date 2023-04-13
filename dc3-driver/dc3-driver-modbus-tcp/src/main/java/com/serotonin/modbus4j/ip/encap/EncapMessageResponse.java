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
package com.serotonin.modbus4j.ip.encap;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpMessageResponse;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>EncapMessageResponse class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class EncapMessageResponse extends EncapMessage implements IpMessageResponse {
    static EncapMessageResponse createEncapMessageResponse(ByteQueue queue) throws ModbusTransportException {
        // Create the modbus response.
        ModbusResponse response = ModbusResponse.createModbusResponse(queue);
        EncapMessageResponse encapResponse = new EncapMessageResponse(response);

        // Check the CRC
        ModbusUtils.checkCRC(encapResponse.modbusMessage, queue);

        return encapResponse;
    }

    /**
     * <p>Constructor for EncapMessageResponse.</p>
     *
     * @param modbusResponse a {@link ModbusResponse} object.
     */
    public EncapMessageResponse(ModbusResponse modbusResponse) {
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
