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
package com.serotonin.modbus4j.exception;

/**
 * <p>ModbusTransportException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
