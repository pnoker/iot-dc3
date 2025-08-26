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
package com.serotonin.modbus4j.exception;

/**
 * <p>IllegalFunctionException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class IllegalFunctionException extends ModbusTransportException {
    private static final long serialVersionUID = -1;

    private final byte functionCode;

    /**
     * <p>Constructor for IllegalFunctionException.</p>
     *
     * @param functionCode a byte.
     * @param slaveId      a int.
     */
    public IllegalFunctionException(byte functionCode, int slaveId) {
        super("Function code: 0x" + Integer.toHexString(functionCode & 0xff), slaveId);
        this.functionCode = functionCode;
    }

    /**
     * <p>Getter for the field <code>functionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getFunctionCode() {
        return functionCode;
    }
}
