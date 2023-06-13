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

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.base.BaseRequestHandler;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingResponseMessage;

/**
 * <p>AsciiRequestHandler class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class AsciiRequestHandler extends BaseRequestHandler {
    /**
     * <p>Constructor for AsciiRequestHandler.</p>
     *
     * @param slave a {@link ModbusSlaveSet} object.
     */
    public AsciiRequestHandler(ModbusSlaveSet slave) {
        super(slave);
    }

    /**
     * {@inheritDoc}
     */
    public OutgoingResponseMessage handleRequest(IncomingRequestMessage req) throws Exception {
        AsciiMessageRequest asciiRequest = (AsciiMessageRequest) req;
        ModbusRequest request = asciiRequest.getModbusRequest();
        ModbusResponse response = handleRequestImpl(request);
        if (response == null)
            return null;
        return new AsciiMessageResponse(response);
    }
}
