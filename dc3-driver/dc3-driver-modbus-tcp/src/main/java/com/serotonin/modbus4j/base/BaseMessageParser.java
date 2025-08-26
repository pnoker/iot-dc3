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
package com.serotonin.modbus4j.base;

import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.messaging.MessageParser;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract BaseMessageParser class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class BaseMessageParser implements MessageParser {
    protected final boolean master;

    /**
     * <p>Constructor for BaseMessageParser.</p>
     *
     * @param master a boolean.
     */
    public BaseMessageParser(boolean master) {
        this.master = master;
    }

    @Override
    public IncomingMessage parseMessage(ByteQueue queue) throws Exception {
        try {
            return parseMessageImpl(queue);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Means that we ran out of data trying to read the message. Just return null.
            return null;
        }
    }

    /**
     * <p>parseMessageImpl.</p>
     *
     * @param queue a {@link ByteQueue} object.
     * @return a {@link IncomingMessage} object.
     * @throws Exception if any.
     */
    abstract protected IncomingMessage parseMessageImpl(ByteQueue queue) throws Exception;
}
