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
package com.serotonin.modbus4j.ip.encap;

import com.serotonin.modbus4j.base.BaseMessageParser;
import com.serotonin.modbus4j.sero.messaging.IncomingMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>EncapMessageParser class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class EncapMessageParser extends BaseMessageParser {
    /**
     * <p>Constructor for EncapMessageParser.</p>
     *
     * @param master a boolean.
     */
    public EncapMessageParser(boolean master) {
        super(master);
    }

    @Override
    protected IncomingMessage parseMessageImpl(ByteQueue queue) throws Exception {
        if (master)
            return EncapMessageResponse.createEncapMessageResponse(queue);
        return EncapMessageRequest.createEncapMessageRequest(queue);
    }
}
