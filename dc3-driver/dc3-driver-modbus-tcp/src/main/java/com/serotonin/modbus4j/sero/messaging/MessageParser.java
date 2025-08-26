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

import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * Interface defining methods that are called when data arrives in the connection.
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public interface MessageParser {
    /**
     * Attempt to parse a message out of the queue. Data in the queue may be discarded if it is unusable (i.e. a start
     * indicator is not found), but otherwise if a message is not found due to the data being incomplete, the method
     * should return null. As additional data arrives, it will be appended to the queue and this method will be called
     * again.
     * <p>
     * Implementations should not modify the queue unless it is safe to do so. No copy of the data is made before
     * calling this method.
     *
     * @param queue the queue from which to access data for the creation of the message
     * @return the message if one was able to be created, or null otherwise.
     * @throws Exception if the data in the queue is sufficient to construct a message, but the message data is invalid, this
     *                   method must throw an exception, or it will keep getting the same data.
     */
    IncomingMessage parseMessage(ByteQueue queue) throws Exception;
}
