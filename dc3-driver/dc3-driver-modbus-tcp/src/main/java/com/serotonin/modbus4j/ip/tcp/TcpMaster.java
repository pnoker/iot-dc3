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
package com.serotonin.modbus4j.ip.tcp;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpMessageResponse;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.ip.encap.EncapMessageParser;
import com.serotonin.modbus4j.ip.encap.EncapMessageRequest;
import com.serotonin.modbus4j.ip.encap.EncapWaitingRoomKeyFactory;
import com.serotonin.modbus4j.ip.xa.XaMessageParser;
import com.serotonin.modbus4j.ip.xa.XaMessageRequest;
import com.serotonin.modbus4j.ip.xa.XaWaitingRoomKeyFactory;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * <p>TcpMaster class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class TcpMaster extends ModbusMaster {

    // Configuration fields.
    private final Log LOG = LogFactory.getLog(TcpMaster.class);
    private short nextTransactionId = 0;
    private final IpParameters ipParameters;
    private final boolean keepAlive;
    private final boolean autoIncrementTransactionId;
    private final Integer lingerTime;

    // Runtime fields.
    private Socket socket;
    private Transport transport;
    private MessageControl conn;


    /**
     * <p>Constructor for TcpMaster.</p>
     *
     * @param params                     a {@link IpParameters} object.
     * @param keepAlive                  a boolean.
     * @param autoIncrementTransactionId a boolean.
     * @param validateResponse           - confirm that requested slave id is the same in the response
     * @param lingerTime                 The setting only affects socket close.
     */
    public TcpMaster(IpParameters params, boolean keepAlive, boolean autoIncrementTransactionId, boolean validateResponse, Integer lingerTime) {
        this.ipParameters = params;
        this.keepAlive = keepAlive;
        this.autoIncrementTransactionId = autoIncrementTransactionId;
        this.lingerTime = lingerTime;
    }

    /**
     * <p>Constructor for TcpMaster.</p>
     * <p>
     * Default to lingerTime disabled
     *
     * @param params                     a {@link IpParameters} object.
     * @param keepAlive                  a boolean.
     * @param autoIncrementTransactionId a boolean.
     * @param validateResponse           - confirm that requested slave id is the same in the response
     */
    public TcpMaster(IpParameters params, boolean keepAlive, boolean autoIncrementTransactionId, boolean validateResponse) {
        this(params, keepAlive, autoIncrementTransactionId, validateResponse, -1);
        //this.ipParameters = params;
        //this.keepAlive = keepAlive;
        //this.autoIncrementTransactionId = autoIncrementTransactionId;
    }

    /**
     * <p>Constructor for TcpMaster.</p>
     * Default to not validating the slave id in responses
     * Default to lingerTime disabled
     *
     * @param params                     a {@link IpParameters} object.
     * @param keepAlive                  a boolean.
     * @param autoIncrementTransactionId a boolean.
     */
    public TcpMaster(IpParameters params, boolean keepAlive, boolean autoIncrementTransactionId) {
        this(params, keepAlive, autoIncrementTransactionId, false, -1);
    }

    /**
     * <p>Constructor for TcpMaster.</p>
     * <p>
     * Default to auto increment transaction id
     * Default to not validating the slave id in responses
     * Default to lingerTime disabled
     *
     * @param params     a {@link IpParameters} object.
     * @param keepAlive  a boolean.
     * @param lingerTime an Integer. The setting only affects socket close.
     */
    public TcpMaster(IpParameters params, boolean keepAlive, Integer lingerTime) {
        this(params, keepAlive, true, false, lingerTime);
    }

    /**
     * <p>Constructor for TcpMaster.</p>
     * <p>
     * Default to auto increment transaction id
     * Default to not validating the slave id in responses
     * Default to lingerTime disabled
     *
     * @param params    a {@link IpParameters} object.
     * @param keepAlive a boolean.
     */
    public TcpMaster(IpParameters params, boolean keepAlive) {
        this(params, keepAlive, true, false, -1);
    }

    /**
     * <p>Setter for the field <code>nextTransactionId</code>.</p>
     *
     * @param id a short.
     */
    public void setNextTransactionId(short id) {
        this.nextTransactionId = id;
    }

    /**
     * <p>Getter for the field <code>nextTransactionId</code>.</p>
     *
     * @return a short.
     */
    protected short getNextTransactionId() {
        return nextTransactionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void init() throws ModbusInitException {
        try {
            if (keepAlive)
                openConnection();
        } catch (Exception e) {
            throw new ModbusInitException(e);
        }
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void destroy() {
        closeConnection();
        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException {
        try {
            // Check if we need to open the connection.
            if (!keepAlive)
                openConnection();

            if (conn == null) {
                LOG.debug("Connection null: " + ipParameters.getPort());
            }

        } catch (Exception e) {
            closeConnection();
            throw new ModbusTransportException(e, request.getSlaveId());
        }

        // Wrap the modbus request in a ip request.
        OutgoingRequestMessage ipRequest;
        if (ipParameters.isEncapsulated())
            ipRequest = new EncapMessageRequest(request);
        else {
            if (autoIncrementTransactionId)
                this.nextTransactionId++;
            ipRequest = new XaMessageRequest(request, getNextTransactionId());
        }

        if (LOG.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipRequest.getMessageData(), 0, ipRequest.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            LOG.debug("Encap Request: " + sb.toString());
        }

        // Send the request to get the response.
        IpMessageResponse ipResponse;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending on port: " + ipParameters.getPort());
        }
        try {
            if (conn == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Connection null: " + ipParameters.getPort());
            }
            ipResponse = (IpMessageResponse) conn.send(ipRequest);
            if (ipResponse == null)
                return null;

            if (LOG.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(), 0, ipResponse.getMessageData().length)) {
                    sb.append(String.format("%02X ", b));
                }
                LOG.debug("Response: " + sb.toString());
            }
            return ipResponse.getModbusResponse();
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.debug("Exception sending message", e);
            if (keepAlive) {
                if (LOG.isDebugEnabled())
                    LOG.debug("KeepAlive - reconnect!");
                // The connection may have been reset, so try to reopen it and attempt the message again.
                try {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Modbus4J: Keep-alive connection may have been reset. Attempting to re-open.");
                    openConnection();
                    ipResponse = (IpMessageResponse) conn.send(ipRequest);
                    if (ipResponse == null)
                        return null;
                    if (LOG.isDebugEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(), 0, ipResponse.getMessageData().length)) {
                            sb.append(String.format("%02X ", b));
                        }
                        LOG.debug("Response: " + sb.toString());
                    }
                    return ipResponse.getModbusResponse();
                } catch (Exception e2) {
                    closeConnection();
                    if (LOG.isDebugEnabled())
                        LOG.debug("Exception re-sending message", e);
                    throw new ModbusTransportException(e2, request.getSlaveId());
                }
            }

            throw new ModbusTransportException(e, request.getSlaveId());
        } finally {
            // Check if we should close the connection.
            if (!keepAlive)
                closeConnection();
        }
    }

    //
    //
    // Private methods
    //
    private void openConnection() throws IOException {
        // Make sure any existing connection is closed.
        closeConnection();

        Integer soLinger = getLingerTime();

        socket = new Socket();
        socket.setSoTimeout(getTimeout());
        if (soLinger == null || soLinger < 0)//any null or negative will disable SO_Linger
            socket.setSoLinger(false, 0);
        else
            socket.setSoLinger(true, soLinger);
        socket.connect(new InetSocketAddress(ipParameters.getHost(), ipParameters.getPort()), getTimeout());
        if (getePoll() != null)
            transport = new EpollStreamTransport(socket.getInputStream(), socket.getOutputStream(), getePoll());
        else
            transport = new StreamTransport(socket.getInputStream(), socket.getOutputStream());

        BaseMessageParser ipMessageParser;
        WaitingRoomKeyFactory waitingRoomKeyFactory;
        if (ipParameters.isEncapsulated()) {
            ipMessageParser = new EncapMessageParser(true);
            waitingRoomKeyFactory = new EncapWaitingRoomKeyFactory();
        } else {
            ipMessageParser = new XaMessageParser(true);
            waitingRoomKeyFactory = new XaWaitingRoomKeyFactory();
        }

        conn = getMessageControl();
        conn.start(transport, ipMessageParser, null, waitingRoomKeyFactory);
        if (getePoll() == null)
            ((StreamTransport) transport).start("Modbus4J TcpMaster");
    }

    private void closeConnection() {
        closeMessageControl(conn);
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            getExceptionHandler().receivedException(e);
        }

        conn = null;
        socket = null;
    }

    /**
     * <p>Getter for the field <code>lingerTime</code>.</p>
     *
     * @return an Integer.
     */
    public Integer getLingerTime() {
        return lingerTime;
    }

}
