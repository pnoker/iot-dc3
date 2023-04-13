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
package com.serotonin.modbus4j.ip.udp;

import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.base.BaseRequestHandler;
import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.encap.EncapMessageParser;
import com.serotonin.modbus4j.ip.encap.EncapRequestHandler;
import com.serotonin.modbus4j.ip.xa.XaMessageParser;
import com.serotonin.modbus4j.ip.xa.XaRequestHandler;
import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.messaging.IncomingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingResponseMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>UdpSlave class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class UdpSlave extends ModbusSlaveSet {
    // Configuration fields
    private final int port;

    // Runtime fields.
    DatagramSocket datagramSocket;
    private final ExecutorService executorService;
    final BaseMessageParser messageParser;
    final BaseRequestHandler requestHandler;

    /**
     * <p>Constructor for UdpSlave.</p>
     *
     * @param encapsulated a boolean.
     */
    public UdpSlave(boolean encapsulated) {
        this(ModbusUtils.TCP_PORT, encapsulated);
    }

    /**
     * <p>Constructor for UdpSlave.</p>
     *
     * @param port         a int.
     * @param encapsulated a boolean.
     */
    public UdpSlave(int port, boolean encapsulated) {
        this.port = port;

        if (encapsulated) {
            messageParser = new EncapMessageParser(false);
            requestHandler = new EncapRequestHandler(this);
        } else {
            messageParser = new XaMessageParser(false);
            requestHandler = new XaRequestHandler(this);
        }

        executorService = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ModbusInitException {
        try {
            datagramSocket = new DatagramSocket(port);

            DatagramPacket datagramPacket;
            while (true) {
                datagramPacket = new DatagramPacket(new byte[1028], 1028);
                datagramSocket.receive(datagramPacket);

                UdpConnectionHandler handler = new UdpConnectionHandler(datagramPacket);
                executorService.execute(handler);
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
        datagramSocket.close();

        // Close the executor service.
        executorService.shutdown();
        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            getExceptionHandler().receivedException(e);
        }
    }

    // int getSlaveId() {
    // return slaveId;
    // }
    //
    // ProcessImage getProcessImage() {
    // return processImage;
    // }

    class UdpConnectionHandler implements Runnable {
        private final DatagramPacket requestPacket;

        UdpConnectionHandler(DatagramPacket requestPacket) {
            this.requestPacket = requestPacket;
        }

        public void run() {
            try {
                ByteQueue requestQueue = new ByteQueue(requestPacket.getData(), 0, requestPacket.getLength());

                // Parse the request data and get the response.
                IncomingMessage request = messageParser.parseMessage(requestQueue);
                OutgoingResponseMessage response = requestHandler.handleRequest((IncomingRequestMessage) request);

                if (response == null)
                    return;

                // Create a response packet.
                byte[] responseData = response.getMessageData();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                        requestPacket.getAddress(), requestPacket.getPort());

                // Send the response back.
                datagramSocket.send(responsePacket);
            } catch (Exception e) {
                getExceptionHandler().receivedException(e);
            }
        }
    }
}
