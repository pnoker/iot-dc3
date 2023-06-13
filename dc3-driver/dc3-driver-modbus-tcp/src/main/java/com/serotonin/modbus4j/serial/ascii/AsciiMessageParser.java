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
package com.serotonin.modbus4j.serial.ascii;

import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>AsciiMessageParser class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class AsciiMessageParser extends BaseMessageParser {
    /**
     * <p>Constructor for AsciiMessageParser.</p>
     *
     * @param master a boolean.
     */
    public AsciiMessageParser(boolean master) {
        super(master);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IncomingMessage parseMessageImpl(ByteQueue queue) throws Exception {
        if (master)
            return AsciiMessageResponse.createAsciiMessageResponse(queue);
        return AsciiMessageRequest.createAsciiMessageRequest(queue);
    }
}
