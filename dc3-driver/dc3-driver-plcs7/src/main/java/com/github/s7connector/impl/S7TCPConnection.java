/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.impl;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.SiemensPLCS;
import com.github.s7connector.exception.S7Exception;
import com.github.s7connector.impl.nodave.Nodave;
import com.github.s7connector.impl.nodave.PLCinterface;
import com.github.s7connector.impl.nodave.TCPConnection;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * TCP_Connection to a S7 PLC
 *
 * @author Thomas Rudin
 * @href http://libnodave.sourceforge.net/
 */
public final class S7TCPConnection extends S7BaseConnection {

    /**
     * The Connection
     */
    private TCPConnection dc;

    /**
     * The Interface
     */
    private PLCinterface di;

    /**
     * The Host to connect to
     */
    private final String host;

    /**
     * The port to connect to
     */
    private final int port;

    /**
     * Rack and slot number
     */
    private final int rack, slot;

    /**
     * Timeout number
     */
    private final int timeout;

    /**
     * The Socket
     */
    private Socket socket;

    /**
     * The connect device type,such as S200
     */
    private SiemensPLCS plcType;

    /**
     * Creates a new Instance to the given host, rack, slot and port
     *
     * @param host
     * @throws S7Exception
     */
    public S7TCPConnection(final String host, final int rack, final int slot, final int port, final int timeout, final SiemensPLCS plcType) throws S7Exception {
        this.host = host;
        this.rack = rack;
        this.slot = slot;
        this.port = port;
        this.timeout = timeout;
        this.plcType = plcType;
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
            switch (this.plcType) {
                case S200:
                    protocol = Nodave.PROTOCOL_ISOTCP243;
                    break;
                case SNon200:
                case S300:
                case S400:
                case S1200:
                case S1500:
                case S200Smart:
                default:
                    protocol = Nodave.PROTOCOL_ISOTCP;
                    break;
            }
            this.di = new PLCinterface(this.socket.getOutputStream(), this.socket.getInputStream(), "IF1",
                    DaveArea.LOCAL.getCode(), // TODO Local MPI-Address?
                    protocol);

            this.dc = new TCPConnection(this.di, this.rack, this.slot);
            final int res = this.dc.connectPLC();
            checkResult(res);

            super.init(this.dc);
        } catch (final Exception e) {
            throw new S7Exception("constructor", e);
        }

    }

}
