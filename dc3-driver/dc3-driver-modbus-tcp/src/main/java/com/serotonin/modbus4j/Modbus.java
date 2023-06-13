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
 * @version 5.0.0
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
     * <p>Getter for the field <code>exceptionHandler</code>.</p>
     *
     * @return a {@link MessagingExceptionHandler} object.
     */
    public MessagingExceptionHandler getExceptionHandler() {
        return exceptionHandler;
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
