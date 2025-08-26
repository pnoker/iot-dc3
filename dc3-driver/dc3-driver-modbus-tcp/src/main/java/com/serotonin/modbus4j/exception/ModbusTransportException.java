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
 * <p>ModbusTransportException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ModbusTransportException extends Exception {
    private static final long serialVersionUID = -1;

    private final int slaveId;

    /**
     * <p>Constructor for ModbusTransportException.</p>
     */
    public ModbusTransportException() {
        this.slaveId = -1;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param slaveId a int.
     */
    public ModbusTransportException(int slaveId) {
        this.slaveId = slaveId;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param message a {@link String} object.
     * @param cause   a {@link Throwable} object.
     * @param slaveId a int.
     */
    public ModbusTransportException(String message, Throwable cause, int slaveId) {
        super(message, cause);
        this.slaveId = slaveId;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param message a {@link String} object.
     * @param slaveId a int.
     */
    public ModbusTransportException(String message, int slaveId) {
        super(message);
        this.slaveId = slaveId;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param message a {@link String} object.
     */
    public ModbusTransportException(String message) {
        super(message);
        this.slaveId = -1;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param cause a {@link Throwable} object.
     */
    public ModbusTransportException(Throwable cause) {
        super(cause);
        this.slaveId = -1;
    }

    /**
     * <p>Constructor for ModbusTransportException.</p>
     *
     * @param cause   a {@link Throwable} object.
     * @param slaveId a int.
     */
    public ModbusTransportException(Throwable cause, int slaveId) {
        super(cause);
        this.slaveId = slaveId;
    }

    /**
     * <p>Getter for the field <code>slaveId</code>.</p>
     *
     * @return a int.
     */
    public int getSlaveId() {
        return slaveId;
    }
}
