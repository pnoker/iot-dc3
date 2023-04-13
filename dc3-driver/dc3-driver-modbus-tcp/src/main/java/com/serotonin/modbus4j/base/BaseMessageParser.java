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
package com.serotonin.modbus4j.base;

import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.messaging.MessageParser;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>Abstract BaseMessageParser class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
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
