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
 * @version 2025.6.0
 */
public class EncapMessageResponse extends EncapMessage implements IpMessageResponse {
    /**
     * <p>Constructor for EncapMessageResponse.</p>
     *
     * @param modbusResponse a {@link ModbusResponse} object.
     */
    public EncapMessageResponse(ModbusResponse modbusResponse) {
        super(modbusResponse);
    }

    static EncapMessageResponse createEncapMessageResponse(ByteQueue queue) throws ModbusTransportException {
        // Create the modbus response.
        ModbusResponse response = ModbusResponse.createModbusResponse(queue);
        EncapMessageResponse encapResponse = new EncapMessageResponse(response);

        // Check the CRC
        ModbusUtils.checkCRC(encapResponse.modbusMessage, queue);

        return encapResponse;
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
