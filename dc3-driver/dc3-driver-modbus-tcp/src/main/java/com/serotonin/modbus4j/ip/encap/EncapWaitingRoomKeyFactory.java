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

package com.serotonin.modbus4j.ip.encap;

import com.serotonin.modbus4j.ip.IpMessage;
import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKey;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKeyFactory;

/**
 * <p>EncapWaitingRoomKeyFactory class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class EncapWaitingRoomKeyFactory implements WaitingRoomKeyFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public WaitingRoomKey createWaitingRoomKey(OutgoingRequestMessage request) {
        return createWaitingRoomKey(((IpMessage) request).getModbusMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaitingRoomKey createWaitingRoomKey(IncomingResponseMessage response) {
        return createWaitingRoomKey(((IpMessage) response).getModbusMessage());
    }

    /**
     * <p>createWaitingRoomKey.</p>
     *
     * @param msg a {@link ModbusMessage} object.
     * @return a {@link WaitingRoomKey} object.
     */
    public WaitingRoomKey createWaitingRoomKey(ModbusMessage msg) {
        return new EncapWaitingRoomKey(msg.getSlaveId(), msg.getFunctionCode());
    }

    class EncapWaitingRoomKey implements WaitingRoomKey {
        private final int slaveId;
        private final byte functionCode;

        public EncapWaitingRoomKey(int slaveId, byte functionCode) {
            this.slaveId = slaveId;
            this.functionCode = functionCode;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + functionCode;
            result = prime * result + slaveId;
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
            EncapWaitingRoomKey other = (EncapWaitingRoomKey) obj;
            if (functionCode != other.functionCode)
                return false;
            if (slaveId != other.slaveId)
                return false;
            return true;
        }
    }
}
