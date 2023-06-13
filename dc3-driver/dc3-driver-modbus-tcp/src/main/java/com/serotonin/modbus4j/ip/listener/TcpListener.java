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

package com.serotonin.modbus4j.ip.listener;

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
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>TcpListener class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class TcpListener extends ModbusMaster {
    // Configuration fields.
    private final Log LOG = LogFactory.getLog(TcpListener.class);
    private short nextTransactionId = 0;
    private short retries = 0;
    private final IpParameters ipParameters;

    // Runtime fields.
    private ServerSocket serverSocket;
    private Socket socket;
    private ExecutorService executorService;
    private ListenerConnectionHandler handler;

    /**
     * <p>Constructor for TcpListener.</p>
     * <p>
     * Will validate response to ensure that slaveId == response slaveId if encapsulated is true
     *
     * @param params a {@link IpParameters} object.
     */
    public TcpListener(IpParameters params) {
        LOG.debug("Creating TcpListener in port " + params.getPort());
        ipParameters = params;
        connected = false;
        validateResponse = ipParameters.isEncapsulated();
        if (LOG.isDebugEnabled())
            LOG.debug("TcpListener created! Port: " + ipParameters.getPort());
    }

    /**
     * Control to validate response to ensure that slaveId == response slaveId
     *
     * @param params           a {@link IpParameters} object.
     * @param validateResponse a boolean.
     */
    public TcpListener(IpParameters params, boolean validateResponse) {
        LOG.debug("Creating TcpListener in port " + params.getPort());
        ipParameters = params;
        connected = false;
        this.validateResponse = validateResponse;
        if (LOG.isDebugEnabled())
            LOG.debug("TcpListener created! Port: " + ipParameters.getPort());
    }

    /**
     * <p>Getter for the field <code>nextTransactionId</code>.</p>
     *
     * @return a short.
     */
    protected short getNextTransactionId() {
        return nextTransactionId++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void init() throws ModbusInitException {
        LOG.debug("Init TcpListener Port: " + ipParameters.getPort());
        executorService = Executors.newCachedThreadPool();
        startListener();
        initialized = true;
        LOG.warn("Initialized Port: " + ipParameters.getPort());
    }

    private void startListener() throws ModbusInitException {
        try {
            if (handler != null) {
                LOG.debug("handler not null!!!");
            }
            handler = new ListenerConnectionHandler(socket);
            LOG.debug("Init handler thread");
            executorService.execute(handler);
        } catch (Exception e) {
            LOG.warn("Error initializing TcpListener ", e);
            throw new ModbusInitException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void destroy() {
        LOG.debug("Destroy TCPListener Port: " + ipParameters.getPort());
        // Close the serverSocket first to prevent new messages.
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            LOG.warn("Error closing socket" + e.getLocalizedMessage());
            getExceptionHandler().receivedException(e);
        }

        // Close all open connections.
        if (handler != null) {
            handler.closeConnection();
        }

        // Terminate Listener
        terminateListener();
        initialized = false;
        LOG.debug("TCPListener destroyed,  Port: " + ipParameters.getPort());
    }

    private void terminateListener() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(300, TimeUnit.MILLISECONDS);
            LOG.debug("Handler Thread terminated,  Port: " + ipParameters.getPort());
        } catch (InterruptedException e) {
            LOG.debug("Error terminating executorService - " + e.getLocalizedMessage());
            getExceptionHandler().receivedException(e);
        }
        handler = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException {

        if (!connected) {
            LOG.debug("No connection in Port: " + ipParameters.getPort());
            throw new ModbusTransportException(new Exception("TCP Listener has no active connection!"),
                    request.getSlaveId());
        }

        if (!initialized) {
            LOG.debug("Listener already terminated " + ipParameters.getPort());
            return null;
        }

        // Wrap the modbus request in a ip request.
        OutgoingRequestMessage ipRequest;
        if (ipParameters.isEncapsulated()) {
            ipRequest = new EncapMessageRequest(request);
            StringBuilder sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipRequest.getMessageData(), 0, ipRequest.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            LOG.debug("Encap Request: " + sb.toString());
        } else {
            ipRequest = new XaMessageRequest(request, getNextTransactionId());
            StringBuilder sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipRequest.getMessageData(), 0, ipRequest.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            LOG.debug("Xa Request: " + sb.toString());
        }

        // Send the request to get the response.
        IpMessageResponse ipResponse;
        try {
            // Send data via handler!
            handler.conn.DEBUG = true;
            ipResponse = (IpMessageResponse) handler.conn.send(ipRequest);
            if (ipResponse == null) {
                throw new ModbusTransportException(new Exception("No valid response from slave!"), request.getSlaveId());
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(), 0, ipResponse.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            LOG.debug("Response: " + sb.toString());
            return ipResponse.getModbusResponse();
        } catch (Exception e) {
            LOG.debug(e.getLocalizedMessage() + ",  Port: " + ipParameters.getPort() + ", retries: " + retries);
            if (retries < 10 && !e.getLocalizedMessage().contains("Broken")) {
                retries++;
            } else {
                /*
                 * To recover from a Broken Pipe, the only way is to restart serverSocket
                 */
                LOG.debug("Restarting Socket,  Port: " + ipParameters.getPort() + ", retries: " + retries);

                // Close the serverSocket first to prevent new messages.
                try {
                    if (serverSocket != null)
                        serverSocket.close();
                } catch (IOException e2) {
                    LOG.debug("Error closing socket" + e2.getLocalizedMessage(), e);
                    getExceptionHandler().receivedException(e2);
                }

                // Close all open connections.
                if (handler != null) {
                    handler.closeConnection();
                    terminateListener();
                }

                if (!initialized) {
                    LOG.debug("Listener already terminated " + ipParameters.getPort());
                    return null;
                }

                executorService = Executors.newCachedThreadPool();
                try {
                    startListener();
                } catch (Exception e2) {
                    LOG.warn("Error trying to restart socket" + e2.getLocalizedMessage(), e);
                    throw new ModbusTransportException(e2, request.getSlaveId());
                }
                retries = 0;
            }
            LOG.warn("Error sending request,  Port: " + ipParameters.getPort() + ", msg: " + e.getMessage());
            // Simple send error!
            throw new ModbusTransportException(e, request.getSlaveId());
        }
    }

    class ListenerConnectionHandler implements Runnable {
        private Socket socket;
        private Transport transport;
        private MessageControl conn;
        private BaseMessageParser ipMessageParser;
        private WaitingRoomKeyFactory waitingRoomKeyFactory;

        public ListenerConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            LOG.debug(" ListenerConnectionHandler::run() ");

            if (ipParameters.isEncapsulated()) {
                ipMessageParser = new EncapMessageParser(true);
                waitingRoomKeyFactory = new EncapWaitingRoomKeyFactory();
            } else {
                ipMessageParser = new XaMessageParser(true);
                waitingRoomKeyFactory = new XaWaitingRoomKeyFactory();
            }

            try {
                acceptConnection();
            } catch (IOException e) {
                LOG.debug("Error in TCP Listener! - " + e.getLocalizedMessage(), e);
                conn.close();
                closeConnection();
                getExceptionHandler().receivedException(new ModbusInitException(e));
            }
        }

        private void acceptConnection() throws IOException, BindException {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!connected) {
                    try {
                        serverSocket = new ServerSocket(ipParameters.getPort());
                        LOG.debug("Start Accept on port: " + ipParameters.getPort());
                        socket = serverSocket.accept();
                        LOG.info("Connected: " + socket.getInetAddress() + ":" + ipParameters.getPort());

                        if (getePoll() != null)
                            transport = new EpollStreamTransport(socket.getInputStream(), socket.getOutputStream(),
                                    getePoll());
                        else
                            transport = new StreamTransport(socket.getInputStream(), socket.getOutputStream());
                        break;
                    } catch (Exception e) {
                        LOG.warn(
                                "Open connection failed on port " + ipParameters.getPort() + ", caused by "
                                        + e.getLocalizedMessage(), e);
                        if (e instanceof SocketTimeoutException) {
                            continue;
                        } else if (e.getLocalizedMessage().contains("closed")) {
                            return;
                        } else if (e instanceof BindException) {
                            closeConnection();
                            throw (BindException) e;
                        }
                    }
                }
            }

            conn = getMessageControl();
            conn.setExceptionHandler(getExceptionHandler());
            conn.DEBUG = true;
            conn.start(transport, ipMessageParser, null, waitingRoomKeyFactory);
            if (getePoll() == null)
                ((StreamTransport) transport).start("Modbus4J TcpMaster");
            connected = true;
        }

        void closeConnection() {
            if (conn != null) {
                LOG.debug("Closing Message Control on port: " + ipParameters.getPort());
                closeMessageControl(conn);
            }

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                LOG.debug("Error closing socket on port " + ipParameters.getPort() + ". " + e.getLocalizedMessage());
                getExceptionHandler().receivedException(new ModbusInitException(e));
            }
            connected = false;
            conn = null;
            socket = null;
        }
    }
}
