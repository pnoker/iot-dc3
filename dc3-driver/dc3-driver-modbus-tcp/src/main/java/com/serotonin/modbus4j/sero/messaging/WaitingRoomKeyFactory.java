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

package com.serotonin.modbus4j.sero.messaging;


/**
 * <p>WaitingRoomKeyFactory interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public interface WaitingRoomKeyFactory {
    /**
     * <p>createWaitingRoomKey.</p>
     *
     * @param request a {@link OutgoingRequestMessage} object.
     * @return a {@link WaitingRoomKey} object.
     */
    WaitingRoomKey createWaitingRoomKey(OutgoingRequestMessage request);

    /**
     * <p>createWaitingRoomKey.</p>
     *
     * @param response a {@link IncomingResponseMessage} object.
     * @return a {@link WaitingRoomKey} object.
     */
    WaitingRoomKey createWaitingRoomKey(IncomingResponseMessage response);
}
