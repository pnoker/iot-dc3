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

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpMessageResponse;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.ip.encap.EncapMessageParser;
import com.serotonin.modbus4j.ip.encap.EncapMessageRequest;
import com.serotonin.modbus4j.ip.xa.XaMessageParser;
import com.serotonin.modbus4j.ip.xa.XaMessageRequest;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

import java.io.IOException;
import java.net.*;

/**
 * <p>UdpMaster class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class UdpMaster extends ModbusMaster {
    private static final int MESSAGE_LENGTH = 1024;

    private short nextTransactionId = 0;
    private final IpParameters ipParameters;

    // Runtime fields.
    private BaseMessageParser messageParser;
    private DatagramSocket socket;

    /**
     * <p>Constructor for UdpMaster.</p>
     * <p>
     * Default to not validating the slave id in responses
     *
     * @param params a {@link IpParameters} object.
     */
    public UdpMaster(IpParameters params) {
        this(params, false);
    }

    /**
     * <p>Constructor for UdpMaster.</p>
     *
     * @param params
     * @param validateResponse - confirm that requested slave id is the same in the response
     */
    public UdpMaster(IpParameters params, boolean validateResponse) {
        ipParameters = params;
        this.validateResponse = validateResponse;
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
    public void init() throws ModbusInitException {
        if (ipParameters.isEncapsulated())
            messageParser = new EncapMessageParser(true);
        else
            messageParser = new XaMessageParser(true);

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(getTimeout());
        } catch (SocketException e) {
            throw new ModbusInitException(e);
        }
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        socket.close();
        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException {
        // Wrap the modbus request in an ip request.
        OutgoingRequestMessage ipRequest;
        if (ipParameters.isEncapsulated())
            ipRequest = new EncapMessageRequest(request);
        else
            ipRequest = new XaMessageRequest(request, getNextTransactionId());

        IpMessageResponse ipResponse;

        try {
            int attempts = getRetries() + 1;

            while (true) {
                // Send the request.
                sendImpl(ipRequest);

                if (!ipRequest.expectsResponse())
                    return null;

                // Receive the response.
                try {
                    ipResponse = receiveImpl();
                } catch (SocketTimeoutException e) {
                    attempts--;
                    if (attempts > 0)
                        // Try again.
                        continue;

                    throw new ModbusTransportException(e, request.getSlaveId());
                }

                // We got the response
                break;
            }

            return ipResponse.getModbusResponse();
        } catch (IOException e) {
            throw new ModbusTransportException(e, request.getSlaveId());
        }
    }

    private void sendImpl(OutgoingRequestMessage request) throws IOException {
        byte[] data = request.getMessageData();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipParameters.getHost()),
                ipParameters.getPort());
        socket.send(packet);
    }

    private IpMessageResponse receiveImpl() throws IOException, ModbusTransportException {
        DatagramPacket packet = new DatagramPacket(new byte[MESSAGE_LENGTH], MESSAGE_LENGTH);
        socket.receive(packet);

        // We could verify that the packet was received from the same address to which the request was sent,
        // but let's not bother with that yet.

        ByteQueue queue = new ByteQueue(packet.getData(), 0, packet.getLength());
        IpMessageResponse response;
        try {
            response = (IpMessageResponse) messageParser.parseMessage(queue);
        } catch (Exception e) {
            throw new ModbusTransportException(e);
        }

        if (response == null)
            throw new ModbusTransportException("Invalid response received");

        return response;
    }
}
