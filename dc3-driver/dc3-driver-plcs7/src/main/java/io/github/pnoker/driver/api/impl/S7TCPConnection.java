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
package io.github.pnoker.driver.api.impl;

import io.github.pnoker.driver.api.DaveArea;
import io.github.pnoker.driver.api.SiemensPLCS;
import io.github.pnoker.driver.api.impl.nodave.Nodave;
import io.github.pnoker.driver.api.impl.nodave.PLCinterface;
import io.github.pnoker.driver.api.impl.nodave.TCPConnection;
import io.github.pnoker.driver.exception.S7Exception;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * TCP_Connection to a S7 PLC
 * <p>
 * 参考：<a href="http://libnodave.sourceforge.net/">http://libnodave.sourceforge.net</a>
 *
 * @author Thomas Rudin
 */
public final class S7TCPConnection extends S7BaseConnection {

    /**
     * The Connection
     */
    private TCPConnection tcpConnection;

    /**
     * The Interface
     */
    private PLCinterface plCinterface;

    /**
     * The Host to connect to
     */
    private final String host;

    /**
     * The port to connect to
     */
    private final int port;

    /**
     * Rack  number
     */
    private final int rack;

    /**
     * Slot number
     */
    private final int slot;

    /**
     * Timeout number
     */
    private final int timeout;

    /**
     * The Socket
     */
    private Socket socket;

    /**
     * To connect device type,such as S200
     */
    private final SiemensPLCS siemensPLCS;

    /**
     * Creates a new Instance to the given host, rack, slot and port
     *
     * @param host        Host
     * @param rack        Rack
     * @param slot        Slot
     * @param port        Port
     * @param timeout     Timeout
     * @param siemensPLCS SiemensPLCS
     * @throws S7Exception S7Exception
     */
    public S7TCPConnection(final String host, final int rack, final int slot, final int port, final int timeout, final SiemensPLCS siemensPLCS) throws S7Exception {
        this.host = host;
        this.rack = rack;
        this.slot = slot;
        this.port = port;
        this.timeout = timeout;
        this.siemensPLCS = siemensPLCS;
        this.setupSocket();
    }

    @Override
    public void close() {
        try {
            this.socket.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        this.close();
    }

    /**
     * Sets up the socket
     */
    private void setupSocket() {
        try {
            this.socket = new Socket();
            this.socket.setSoTimeout(2000);
            this.socket.connect(new InetSocketAddress(this.host, this.port), this.timeout);

            //select the plc interface protocol by the plcsType
            int protocol;
            switch (this.siemensPLCS) {
                case S_200:
                    protocol = Nodave.PROTOCOL_ISOTCP243;
                    break;
                case S_NON_200:
                case S_300:
                case S_400:
                case S_1200:
                case S_1500:
                case S_200_SMART:
                default:
                    protocol = Nodave.PROTOCOL_ISOTCP;
                    break;
            }
            this.plCinterface = new PLCinterface(this.socket.getOutputStream(), this.socket.getInputStream(), "IF1",
                    DaveArea.LOCAL.getCode(), // TODO Local MPI-Address?
                    protocol);

            this.tcpConnection = new TCPConnection(this.plCinterface, this.rack, this.slot);
            final int res = this.tcpConnection.connectPLC();
            checkResult(res);

            super.init(this.tcpConnection);
        } catch (final Exception e) {
            throw new S7Exception("constructor", e);
        }

    }

}
