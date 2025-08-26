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

package com.serotonin.modbus4j.serial;

import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKey;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKeyFactory;

/**
 * <p>SerialWaitingRoomKeyFactory class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class SerialWaitingRoomKeyFactory implements WaitingRoomKeyFactory {
    private static final Sync sync = new Sync();

    @Override
    public WaitingRoomKey createWaitingRoomKey(OutgoingRequestMessage request) {
        return sync;
    }

    @Override
    public WaitingRoomKey createWaitingRoomKey(IncomingResponseMessage response) {
        return sync;
    }

    static class Sync implements WaitingRoomKey {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            return true;
        }
    }
}
