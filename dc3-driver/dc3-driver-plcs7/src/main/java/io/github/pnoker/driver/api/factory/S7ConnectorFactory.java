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
     * TCP Connection builder
     */
    public static class TCPConnectionBuilder {

        private String host;

        private final SiemensPLCS plcsType;

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

}
