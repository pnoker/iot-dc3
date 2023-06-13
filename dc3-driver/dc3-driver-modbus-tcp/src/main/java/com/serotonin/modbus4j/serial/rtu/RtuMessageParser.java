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
package com.serotonin.modbus4j.serial.rtu;

import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * Message parser implementation for RTU encoding. Primary reference for the ordering of CRC bytes. Also provides
 * handling of incomplete messages.
 *
 * @author mlohbihler
 * @version 5.0.0
 */
public class RtuMessageParser extends BaseMessageParser {
    /**
     * <p>Constructor for RtuMessageParser.</p>
     *
     * @param master a boolean.
     */
    public RtuMessageParser(boolean master) {
        super(master);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IncomingMessage parseMessageImpl(ByteQueue queue) throws Exception {
        if (master)
            return RtuMessageResponse.createRtuMessageResponse(queue);
        return RtuMessageRequest.createRtuMessageRequest(queue);
    }
    //
    // public static void main(String[] args) throws Exception {
    // ByteQueue queue = new ByteQueue(new byte[] { 5, 3, 2, 0, (byte) 0xdc, (byte) 0x48, (byte) 0x1d, 0 });
    // RtuMessageParser p = new RtuMessageParser(false);
    // System.out.println(p.parseResponse(queue));
    // }
}
