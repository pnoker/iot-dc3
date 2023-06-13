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

package com.serotonin.modbus4j.ip.xa;

import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKey;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKeyFactory;

/**
 * <p>XaWaitingRoomKeyFactory class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class XaWaitingRoomKeyFactory implements WaitingRoomKeyFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public WaitingRoomKey createWaitingRoomKey(OutgoingRequestMessage request) {
        return createWaitingRoomKey((XaMessage) request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaitingRoomKey createWaitingRoomKey(IncomingResponseMessage response) {
        return createWaitingRoomKey((XaMessage) response);
    }

    /**
     * <p>createWaitingRoomKey.</p>
     *
     * @param msg a {@link XaMessage} object.
     * @return a {@link WaitingRoomKey} object.
     */
    public WaitingRoomKey createWaitingRoomKey(XaMessage msg) {
        return new XaWaitingRoomKey(msg.getTransactionId(), msg.getModbusMessage());
    }

    class XaWaitingRoomKey implements WaitingRoomKey {
        private final int transactionId;
        private final int slaveId;
        private final byte functionCode;

        public XaWaitingRoomKey(int transactionId, ModbusMessage msg) {
            this.transactionId = transactionId;
            this.slaveId = msg.getSlaveId();
            this.functionCode = msg.getFunctionCode();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + functionCode;
            result = prime * result + slaveId;
            result = prime * result + transactionId;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            XaWaitingRoomKey other = (XaWaitingRoomKey) obj;
            if (functionCode != other.functionCode)
                return false;
            if (slaveId != other.slaveId)
                return false;
            if (transactionId != other.transactionId)
                return false;
            return true;
        }
    }
}
