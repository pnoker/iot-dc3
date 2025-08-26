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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper to further aid in abstracting Modbus4J from a serial port implementation
 *
 * @author Terry Packer
 * @version 2025.6.0
 */
public interface SerialPortWrapper {

    /**
     * Close the Serial Port
     *
     * @throws Exception if any.
     */
    void close() throws Exception;

    /**
     * <p>open.</p>
     *
     * @throws Exception if any.
     */
    void open() throws Exception;

    /**
     * Return the input stream for an open port
     *
     * @return a {@link InputStream} object.
     */
    InputStream getInputStream();

    /**
     * Return the output stream for an open port
     *
     * @return a {@link OutputStream} object.
     */
    OutputStream getOutputStream();

    /**
     * <p>getBaudRate.</p>
     *
     * @return a int.
     */
    int getBaudRate();

    /**
     * <p>getDataBits.</p>
     *
     * @return a int.
     */
    int getDataBits();

    /**
     * <p>getStopBits.</p>
     *
     * @return a int.
     */
    int getStopBits();

    /**
     * <p>getParity.</p>
     *
     * @return a int.
     */
    int getParity();


}
