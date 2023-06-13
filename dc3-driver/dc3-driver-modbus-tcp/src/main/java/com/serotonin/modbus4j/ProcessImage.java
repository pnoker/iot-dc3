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

import com.serotonin.modbus4j.exception.IllegalDataAddressException;

/**
 * Used by slave implementors. Provides an interface by which slaves can easily manage data.
 *
 * @author mlohbihler
 * @version 5.0.0
 */
public interface ProcessImage {
    /**
     * <p>getSlaveId.</p>
     *
     * @return a int.
     */
    int getSlaveId();

    //
    // /
    // / Coils
    // /
    //

    /**
     * Returns the current value of the coil for the given offset.
     *
     * @param offset a int.
     * @return the value of the coil
     * @throws IllegalDataAddressException if any.
     */
    boolean getCoil(int offset) throws IllegalDataAddressException;

    /**
     * Used internally for setting the value of the coil.
     *
     * @param offset a int.
     * @param value  a boolean.
     */
    void setCoil(int offset, boolean value);

    /**
     * Used to set the coil as a result of a write command from the master.
     *
     * @param offset a int.
     * @param value  a boolean.
     * @throws IllegalDataAddressException if any.
     */
    void writeCoil(int offset, boolean value) throws IllegalDataAddressException;

    //
    // /
    // / Inputs
    // /
    //

    /**
     * Returns the current value of the input for the given offset.
     *
     * @param offset a int.
     * @return the value of the input
     * @throws IllegalDataAddressException if any.
     */
    boolean getInput(int offset) throws IllegalDataAddressException;

    /**
     * Used internally for setting the value of the input.
     *
     * @param offset a int.
     * @param value  a boolean.
     */
    void setInput(int offset, boolean value);

    //
    // /
    // / Holding registers
    // /
    //

    /**
     * Returns the current value of the holding register for the given offset.
     *
     * @param offset a int.
     * @return the value of the register
     * @throws IllegalDataAddressException if any.
     */
    short getHoldingRegister(int offset) throws IllegalDataAddressException;

    /**
     * Used internally for setting the value of the holding register.
     *
     * @param offset a int.
     * @param value  a short.
     */
    void setHoldingRegister(int offset, short value);

    /**
     * Used to set the holding register as a result of a write command from the master.
     *
     * @param offset a int.
     * @param value  a short.
     * @throws IllegalDataAddressException if any.
     */
    void writeHoldingRegister(int offset, short value) throws IllegalDataAddressException;

    //
    // /
    // / Input registers
    // /
    //

    /**
     * Returns the current value of the input register for the given offset.
     *
     * @param offset a int.
     * @return the value of the register
     * @throws IllegalDataAddressException if any.
     */
    short getInputRegister(int offset) throws IllegalDataAddressException;

    /**
     * Used internally for setting the value of the input register.
     *
     * @param offset a int.
     * @param value  a short.
     */
    void setInputRegister(int offset, short value);

    //
    // /
    // / Exception status
    // /
    //

    /**
     * Returns the current value of the exception status.
     *
     * @return the current value of the exception status.
     */
    byte getExceptionStatus();

    //
    // /
    // / Report slave id
    // /
    //

    /**
     * Returns the data for the report slave id command.
     *
     * @return the data for the report slave id command.
     */
    byte[] getReportSlaveIdData();
}
