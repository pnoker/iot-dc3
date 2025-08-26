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
package com.serotonin.modbus4j.serial.rtu;

import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * Message parser implementation for RTU encoding. Primary reference for the ordering of CRC bytes. Also provides
 * handling of incomplete messages.
 *
 * @author mlohbihler
 * @version 2025.6.0
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
