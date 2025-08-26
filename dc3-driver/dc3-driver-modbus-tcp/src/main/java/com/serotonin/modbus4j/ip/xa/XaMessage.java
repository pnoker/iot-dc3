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
package com.serotonin.modbus4j.ip.xa;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.ip.IpMessage;
import com.serotonin.modbus4j.msg.ModbusMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;

/**
 * <p>XaMessage class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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

    @Override
    public ModbusMessage getModbusMessage() {
        return modbusMessage;
    }

    @Override
    public String toString() {
        return "XaMessage [transactionId=" + transactionId + ", message=" + modbusMessage + "]";
    }
}
