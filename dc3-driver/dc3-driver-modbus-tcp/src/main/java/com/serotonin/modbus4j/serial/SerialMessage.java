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

package com.serotonin.modbus4j.serial;

import com.serotonin.modbus4j.msg.ModbusMessage;

/**
 * <p>Abstract SerialMessage class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class SerialMessage {
    protected final ModbusMessage modbusMessage;

    /**
     * <p>Constructor for SerialMessage.</p>
     *
     * @param modbusMessage a {@link ModbusMessage} object.
     */
    public SerialMessage(ModbusMessage modbusMessage) {
        this.modbusMessage = modbusMessage;
    }

    /**
     * <p>Getter for the field <code>modbusMessage</code>.</p>
     *
     * @return a {@link ModbusMessage} object.
     */
    public ModbusMessage getModbusMessage() {
        return modbusMessage;
    }

    @Override
    public String toString() {
        return "SerialMessage [modbusMessage=" + modbusMessage + "]";
    }
}
