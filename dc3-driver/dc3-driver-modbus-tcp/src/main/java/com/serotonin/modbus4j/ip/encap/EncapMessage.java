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
package com.serotonin.modbus4j.ip.encap;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.ip.IpMessage;
import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>EncapMessage class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class EncapMessage extends IpMessage {
    /**
     * <p>Constructor for EncapMessage.</p>
     *
     * @param modbusMessage a {@link ModbusMessage} object.
     */
    public EncapMessage(ModbusMessage modbusMessage) {
        super(modbusMessage);
    }

    /**
     * <p>getMessageData.</p>
     *
     * @return an array of {@link byte} objects.
     */
    public byte[] getMessageData() {
        ByteQueue msgQueue = new ByteQueue();

        // Write the particular message.
        modbusMessage.write(msgQueue);

        // Write the CRC
        ModbusUtils.pushShort(msgQueue, ModbusUtils.calculateCRC(modbusMessage));

        // Return the data.
        return msgQueue.popAll();
    }
}
