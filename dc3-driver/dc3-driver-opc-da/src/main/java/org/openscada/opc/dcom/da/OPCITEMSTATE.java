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

package org.openscada.opc.dcom.da;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.FILETIME;

public class OPCITEMSTATE {
    private int _clientHandle = 0;

    private FILETIME _timestamp = null;

    private short _quality = 0;

    private short _reserved = 0;

    private JIVariant _value = null;

    public static JIStruct getStruct() throws JIException {
        JIStruct struct = new JIStruct();

        struct.addMember(Integer.class);
        struct.addMember(FILETIME.getStruct());
        struct.addMember(Short.class);
        struct.addMember(Short.class);
        struct.addMember(JIVariant.class);

        return struct;
    }

    public static OPCITEMSTATE fromStruct(final JIStruct struct) {
        OPCITEMSTATE itemState = new OPCITEMSTATE();

        itemState.setClientHandle((Integer) struct.getMember(0));
        itemState.setTimestamp(FILETIME.fromStruct((JIStruct) struct.getMember(1)));
        itemState.setQuality((Short) struct.getMember(2));
        itemState.setReserved((Short) struct.getMember(3));
        itemState.setValue((JIVariant) struct.getMember(4));

        return itemState;
    }

    public int getClientHandle() {
        return this._clientHandle;
    }

    public void setClientHandle(final int clientHandle) {
        this._clientHandle = clientHandle;
    }

    public short getQuality() {
        return this._quality;
    }

    public void setQuality(final short quality) {
        this._quality = quality;
    }

    public short getReserved() {
        return this._reserved;
    }

    public void setReserved(final short reserved) {
        this._reserved = reserved;
    }

    public FILETIME getTimestamp() {
        return this._timestamp;
    }

    public void setTimestamp(final FILETIME timestamp) {
        this._timestamp = timestamp;
    }

    public JIVariant getValue() {
        return this._value;
    }

    public void setValue(final JIVariant value) {
        this._value = value;
    }
}
