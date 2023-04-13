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
package com.serotonin.modbus4j.serial;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper to further aid in abstracting Modbus4J from a serial port implementation
 *
 * @author Terry Packer
 * @version 5.0.0
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
