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

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.sero.messaging.StreamTransport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Abstract SerialSlave class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class SerialSlave extends ModbusSlaveSet {

    private final Log LOG = LogFactory.getLog(SerialSlave.class);

    // Runtime fields
    private SerialPortWrapper wrapper;
    protected StreamTransport transport;

    /**
     * <p>Constructor for SerialSlave.</p>
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     */
    public SerialSlave(SerialPortWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ModbusInitException {
        try {

            wrapper.open();

            transport = new StreamTransport(wrapper.getInputStream(), wrapper.getOutputStream());
        } catch (Exception e) {
            throw new ModbusInitException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            wrapper.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
