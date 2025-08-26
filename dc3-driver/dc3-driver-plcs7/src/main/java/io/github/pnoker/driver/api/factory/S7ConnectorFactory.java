/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.driver.api.factory;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.SiemensPLCS;
import io.github.pnoker.driver.api.impl.S7TCPConnection;

/**
 * S7 connector factory, currently only for TCP connections
 *
 * @author Thomas Rudin
 */
public class S7ConnectorFactory {

    private S7ConnectorFactory() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * @param type choose a siemens plc type to build a tcp connector.
     * @return returns a new TCP connection builder
     */
    public static TCPConnectionBuilder buildTCPConnector(SiemensPLCS type) {
        return new TCPConnectionBuilder(type);
    }

    public static TCPConnectionBuilder buildTCPConnector() {
        return new TCPConnectionBuilder(SiemensPLCS.S_NON_200);
    }

    /**
     * TCP Connection builder
     */
    public static class TCPConnectionBuilder {

        private final SiemensPLCS plcsType;
        private String host;
        private int rack = 0;
        private int slot = 2;
        private int port = 102;
        private int timeout = 2000;

        TCPConnectionBuilder(SiemensPLCS type) {
            this.plcsType = type;
        }

        /**
         * Builds a connection with given params
         *
         * @return S7Connector
         */
        public S7Connector build() {
            return new S7TCPConnection(this.host, this.rack, this.slot, this.port, this.timeout, this.plcsType);
        }

        /**
         * use hostname/ip
         *
         * @param host Host
         * @return TCPConnectionBuilder
         */
        public TCPConnectionBuilder withHost(final String host) {
            this.host = host;
            return this;
        }

        /**
         * use port, default is 102
         *
         * @param port Port
         * @return TCPConnectionBuilder
         */
        public TCPConnectionBuilder withPort(final int port) {
            this.port = port;
            return this;
        }

        /**
         * use rack, default is 0
         *
         * @param rack Rack
         * @return TCPConnectionBuilder
         */
        public TCPConnectionBuilder withRack(final int rack) {
            this.rack = rack;
            return this;
        }

        /**
         * use slot, default is 2
         *
         * @param slot Slot
         * @return TCPConnectionBuilder
         */
        public TCPConnectionBuilder withSlot(final int slot) {
            this.slot = slot;
            return this;
        }

        /**
         * use timeout, default is 2000
         *
         * @param timeout Timeout
         * @return TCPConnectionBuilder
         */
        public TCPConnectionBuilder withTimeout(final int timeout) {
            this.timeout = timeout;
            return this;
        }

    }

}
