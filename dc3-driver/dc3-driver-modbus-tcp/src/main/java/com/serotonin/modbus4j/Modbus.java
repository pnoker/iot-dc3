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
package com.serotonin.modbus4j;

import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.DefaultMessagingExceptionHandler;
import com.serotonin.modbus4j.sero.messaging.MessagingExceptionHandler;

/**
 * Base level for masters and slaves/listeners
 * <p>
 * TODO: - handle echoing in RS485
 *
 * @author mlohbihler
 * @version 2025.6.0
 */
public class Modbus {
    /**
     * Constant <code>DEFAULT_MAX_READ_BIT_COUNT=2000</code>
     */
    public static final int DEFAULT_MAX_READ_BIT_COUNT = 2000;
    /**
     * Constant <code>DEFAULT_MAX_READ_REGISTER_COUNT=125</code>
     */
    public static final int DEFAULT_MAX_READ_REGISTER_COUNT = 125;
    /**
     * Constant <code>DEFAULT_MAX_WRITE_REGISTER_COUNT=120</code>
     */
    public static final int DEFAULT_MAX_WRITE_REGISTER_COUNT = 120;

    private MessagingExceptionHandler exceptionHandler = new DefaultMessagingExceptionHandler();

    private int maxReadBitCount = DEFAULT_MAX_READ_BIT_COUNT;
    private int maxReadRegisterCount = DEFAULT_MAX_READ_REGISTER_COUNT;
    private int maxWriteRegisterCount = DEFAULT_MAX_WRITE_REGISTER_COUNT;

    /**
     * <p>getMaxReadCount.</p>
     *
     * @param registerRange a int.
     * @return a int.
     */
    public int getMaxReadCount(int registerRange) {
        switch (registerRange) {
            case RegisterRange.COIL_STATUS:
            case RegisterRange.INPUT_STATUS:
                return maxReadBitCount;
            case RegisterRange.HOLDING_REGISTER:
            case RegisterRange.INPUT_REGISTER:
                return maxReadRegisterCount;
        }
        return -1;
    }

    /**
     * <p>validateNumberOfBits.</p>
     *
     * @param bits a int.
     * @throws ModbusTransportException if any.
     */
    public void validateNumberOfBits(int bits) throws ModbusTransportException {
        if (bits < 1 || bits > maxReadBitCount)
            throw new ModbusTransportException("Invalid number of bits: " + bits);
    }

    /**
     * <p>validateNumberOfRegisters.</p>
     *
     * @param registers a int.
     * @throws ModbusTransportException if any.
     */
    public void validateNumberOfRegisters(int registers) throws ModbusTransportException {
        if (registers < 1 || registers > maxReadRegisterCount)
            throw new ModbusTransportException("Invalid number of registers: " + registers);
    }

    /**
     * <p>Getter for the field <code>exceptionHandler</code>.</p>
     *
     * @return a {@link MessagingExceptionHandler} object.
     */
    public MessagingExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * <p>Setter for the field <code>exceptionHandler</code>.</p>
     *
     * @param exceptionHandler a {@link MessagingExceptionHandler} object.
     */
    public void setExceptionHandler(MessagingExceptionHandler exceptionHandler) {
        if (exceptionHandler == null)
            this.exceptionHandler = new DefaultMessagingExceptionHandler();
        else
            this.exceptionHandler = exceptionHandler;
    }

    /**
     * <p>Getter for the field <code>maxReadBitCount</code>.</p>
     *
     * @return a int.
     */
    public int getMaxReadBitCount() {
        return maxReadBitCount;
    }

    /**
     * <p>Setter for the field <code>maxReadBitCount</code>.</p>
     *
     * @param maxReadBitCount a int.
     */
    public void setMaxReadBitCount(int maxReadBitCount) {
        this.maxReadBitCount = maxReadBitCount;
    }

    /**
     * <p>Getter for the field <code>maxReadRegisterCount</code>.</p>
     *
     * @return a int.
     */
    public int getMaxReadRegisterCount() {
        return maxReadRegisterCount;
    }

    /**
     * <p>Setter for the field <code>maxReadRegisterCount</code>.</p>
     *
     * @param maxReadRegisterCount a int.
     */
    public void setMaxReadRegisterCount(int maxReadRegisterCount) {
        this.maxReadRegisterCount = maxReadRegisterCount;
    }

    /**
     * <p>Getter for the field <code>maxWriteRegisterCount</code>.</p>
     *
     * @return a int.
     */
    public int getMaxWriteRegisterCount() {
        return maxWriteRegisterCount;
    }

    /**
     * <p>Setter for the field <code>maxWriteRegisterCount</code>.</p>
     *
     * @param maxWriteRegisterCount a int.
     */
    public void setMaxWriteRegisterCount(int maxWriteRegisterCount) {
        this.maxWriteRegisterCount = maxWriteRegisterCount;
    }
}
