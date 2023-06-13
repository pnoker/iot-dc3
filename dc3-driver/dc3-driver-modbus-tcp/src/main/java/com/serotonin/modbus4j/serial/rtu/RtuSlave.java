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

import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import com.serotonin.modbus4j.serial.SerialSlave;
import com.serotonin.modbus4j.sero.messaging.MessageControl;

import java.io.IOException;

/**
 * <p>RtuSlave class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class RtuSlave extends SerialSlave {
    // Runtime fields
    private MessageControl conn;

    /**
     * <p>Constructor for RtuSlave.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     */
    public RtuSlave(SerialPortWrapper wrapper) {
        super(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ModbusInitException {
        super.start();

        RtuMessageParser rtuMessageParser = new RtuMessageParser(false);
        RtuRequestHandler rtuRequestHandler = new RtuRequestHandler(this);

        conn = new MessageControl();
        conn.setExceptionHandler(getExceptionHandler());

        try {
            conn.start(transport, rtuMessageParser, rtuRequestHandler, null);
            transport.start("Modbus RTU slave");
        } catch (IOException e) {
            throw new ModbusInitException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        conn.close();
        super.stop();
    }
}
