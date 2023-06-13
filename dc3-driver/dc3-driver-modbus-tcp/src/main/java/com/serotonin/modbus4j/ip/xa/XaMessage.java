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
package com.serotonin.modbus4j.ip.xa;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.ip.IpMessage;
import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>XaMessage class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class XaMessage extends IpMessage {
    protected final int transactionId;

    /**
     * <p>Constructor for XaMessage.</p>
     *
     * @param modbusMessage a {@link ModbusMessage} object.
     * @param transactionId a int.
     */
    public XaMessage(ModbusMessage modbusMessage, int transactionId) {
        super(modbusMessage);
        this.transactionId = transactionId;
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

        // Create the XA message
        ByteQueue xaQueue = new ByteQueue();
        ModbusUtils.pushShort(xaQueue, transactionId);
        ModbusUtils.pushShort(xaQueue, ModbusUtils.IP_PROTOCOL_ID);
        ModbusUtils.pushShort(xaQueue, msgQueue.size());
        xaQueue.push(msgQueue);

        // Return the data.
        return xaQueue.popAll();
    }

    /**
     * <p>Getter for the field <code>transactionId</code>.</p>
     *
     * @return a int.
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModbusMessage getModbusMessage() {
        return modbusMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "XaMessage [transactionId=" + transactionId + ", message=" + modbusMessage + "]";
    }
}
