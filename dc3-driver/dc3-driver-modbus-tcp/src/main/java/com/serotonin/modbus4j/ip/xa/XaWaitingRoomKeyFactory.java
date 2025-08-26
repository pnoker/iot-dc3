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
 * @version 2025.6.0
 */
public class XaWaitingRoomKeyFactory implements WaitingRoomKeyFactory {
    @Override
    public WaitingRoomKey createWaitingRoomKey(OutgoingRequestMessage request) {
        return createWaitingRoomKey((XaMessage) request);
    }

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
