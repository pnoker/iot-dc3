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

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.sero.messaging.EpollStreamTransport;
import com.serotonin.modbus4j.sero.messaging.MessageControl;
import com.serotonin.modbus4j.sero.messaging.StreamTransport;
import com.serotonin.modbus4j.sero.messaging.Transport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Abstract SerialMaster class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class SerialMaster extends ModbusMaster {

    private static final int RETRY_PAUSE_START = 50;
    private static final int RETRY_PAUSE_MAX = 1000;

    private final Log LOG = LogFactory.getLog(SerialMaster.class);

    // Runtime fields.
    protected boolean serialPortOpen;
    protected SerialPortWrapper wrapper;
    protected Transport transport;


    /**
     * <p>Constructor for SerialMaster.</p>
     * <p>
     * Default to validating the slave id in responses
     *
     * @param wrapper a {@link SerialPortWrapper} object.
     */
    public SerialMaster(SerialPortWrapper wrapper) {
        this(wrapper, true);
    }

    /**
     * <p>Constructor for SerialMaster.</p>
     *
     * @param wrapper          a {@link SerialPortWrapper} object.
     * @param validateResponse - confirm that requested slave id is the same in the response
     */
    public SerialMaster(SerialPortWrapper wrapper, boolean validateResponse) {
        this.wrapper = wrapper;
        this.validateResponse = validateResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ModbusInitException {
        try {
            this.openConnection(null);
        } catch (Exception e) {
            throw new ModbusInitException(e);
        }
    }

    /**
     * Open the serial port and initialize the transport, ensure
     * connection is closed first
     *
     * @param conn
     * @throws Exception
     */
    protected void openConnection(MessageControl toClose) throws Exception {
        // Make sure any existing connection is closed.
        closeConnection(toClose);

        // Try 'retries' times to get the socket open.
        int retries = getRetries();
        int retryPause = RETRY_PAUSE_START;
        while (true) {
            try {
                this.wrapper.open();
                this.serialPortOpen = true;
                if (getePoll() != null) {
                    transport = new EpollStreamTransport(wrapper.getInputStream(),
                            wrapper.getOutputStream(),
                            getePoll());
                } else {
                    transport = new StreamTransport(wrapper.getInputStream(),
                            wrapper.getOutputStream());
                }
                break;
            } catch (Exception e) {
                //Ensure port is closed before we try to reopen or bail out
                close();

                if (retries <= 0)
                    throw e;

                retries--;

                // Pause for a bit.
                try {
                    Thread.sleep(retryPause);
                } catch (InterruptedException e1) {
                    // ignore
                }
                retryPause *= 2;
                if (retryPause > RETRY_PAUSE_MAX)
                    retryPause = RETRY_PAUSE_MAX;
            }
        }
    }

    /**
     * Close serial port
     *
     * @param conn
     */
    protected void closeConnection(MessageControl conn) {
        closeMessageControl(conn);
        try {
            if (serialPortOpen) {
                wrapper.close();
                serialPortOpen = false;
            }
        } catch (Exception e) {
            getExceptionHandler().receivedException(e);
        }

        transport = null;
    }

    /**
     * <p>close.</p>
     */
    public void close() {
        try {
            wrapper.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
