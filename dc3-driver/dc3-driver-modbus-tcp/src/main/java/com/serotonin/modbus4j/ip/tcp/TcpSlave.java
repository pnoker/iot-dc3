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

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.base.BaseRequestHandler;
import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.encap.EncapMessageParser;
import com.serotonin.modbus4j.ip.encap.EncapRequestHandler;
import com.serotonin.modbus4j.ip.xa.XaMessageParser;
import com.serotonin.modbus4j.ip.xa.XaRequestHandler;
import com.serotonin.modbus4j.sero.messaging.MessageControl;
import com.serotonin.modbus4j.sero.messaging.TestableTransport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>TcpSlave class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class TcpSlave extends ModbusSlaveSet {
    // Configuration fields
    private final int port;
    final boolean encapsulated;

    // Runtime fields.
    private ServerSocket serverSocket;
    final ExecutorService executorService;
    final List<TcpConnectionHandler> listConnections = new ArrayList<>();

    /**
     * <p>Constructor for TcpSlave.</p>
     *
     * @param encapsulated a boolean.
     */
    public TcpSlave(boolean encapsulated) {
        this(ModbusUtils.TCP_PORT, encapsulated);
    }

    /**
     * <p>Constructor for TcpSlave.</p>
     *
     * @param port         a int.
     * @param encapsulated a boolean.
     */
    public TcpSlave(int port, boolean encapsulated) {
        this.port = port;
        this.encapsulated = encapsulated;
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ModbusInitException {
        try {
            serverSocket = new ServerSocket(port);

            Socket socket;
            while (true) {
                socket = serverSocket.accept();
                TcpConnectionHandler handler = new TcpConnectionHandler(socket);
                executorService.execute(handler);
                synchronized (listConnections) {
                    listConnections.add(handler);
                }
            }
        } catch (IOException e) {
            throw new ModbusInitException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        // Close the socket first to prevent new messages.
        try {
            serverSocket.close();
        } catch (IOException e) {
            getExceptionHandler().receivedException(e);
        }

        // Close all open connections.
        synchronized (listConnections) {
            for (TcpConnectionHandler tch : listConnections)
                tch.kill();
            listConnections.clear();
        }

        // Now close the executor service.
        executorService.shutdown();
        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            getExceptionHandler().receivedException(e);
        }
    }

    class TcpConnectionHandler implements Runnable {
        private final Socket socket;
        private TestableTransport transport;
        private MessageControl conn;

        TcpConnectionHandler(Socket socket) throws ModbusInitException {
            this.socket = socket;
            try {
                transport = new TestableTransport(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException e) {
                throw new ModbusInitException(e);
            }
        }

        @Override
        public void run() {
            BaseMessageParser messageParser;
            BaseRequestHandler requestHandler;

            if (encapsulated) {
                messageParser = new EncapMessageParser(false);
                requestHandler = new EncapRequestHandler(TcpSlave.this);
            } else {
                messageParser = new XaMessageParser(false);
                requestHandler = new XaRequestHandler(TcpSlave.this);
            }

            conn = new MessageControl();
            conn.setExceptionHandler(getExceptionHandler());

            try {
                conn.start(transport, messageParser, requestHandler, null);
                executorService.execute(transport);
            } catch (IOException e) {
                getExceptionHandler().receivedException(new ModbusInitException(e));
            }

            // Monitor the socket to detect when it gets closed.
            while (true) {
                try {
                    transport.testInputStream();
                } catch (IOException e) {
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // no op
                }
            }

            conn.close();
            kill();
            synchronized (listConnections) {
                listConnections.remove(this);
            }
        }

        void kill() {
            try {
                socket.close();
            } catch (IOException e) {
                getExceptionHandler().receivedException(new ModbusInitException(e));
            }
        }
    }
}
