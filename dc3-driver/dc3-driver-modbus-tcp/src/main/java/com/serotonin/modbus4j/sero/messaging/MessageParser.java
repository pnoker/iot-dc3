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

package com.serotonin.modbus4j.sero.messaging;

import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * Interface defining methods that are called when data arrives in the connection.
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
