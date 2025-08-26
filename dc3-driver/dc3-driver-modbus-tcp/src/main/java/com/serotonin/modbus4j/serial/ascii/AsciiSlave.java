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
package com.serotonin.modbus4j.serial.ascii;

import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.serial.SerialPortWrapper;
import com.serotonin.modbus4j.serial.SerialSlave;
import com.serotonin.modbus4j.sero.messaging.MessageControl;

import java.io.IOException;

/**
 * <p>AsciiSlave class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class AsciiSlave extends SerialSlave {
    private MessageControl conn;

    /**
     * <p>Constructor for AsciiSlave.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     */
    public AsciiSlave(SerialPortWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() throws ModbusInitException {
        super.start();

        AsciiMessageParser asciiMessageParser = new AsciiMessageParser(false);
        AsciiRequestHandler asciiRequestHandler = new AsciiRequestHandler(this);

        conn = new MessageControl();
        conn.setExceptionHandler(getExceptionHandler());

        try {
            conn.start(transport, asciiMessageParser, asciiRequestHandler, null);
            transport.start("Modbus ASCII slave");
        } catch (IOException e) {
            throw new ModbusInitException(e);
        }
    }

    @Override
    public void stop() {
        conn.close();
        super.stop();
    }
}
