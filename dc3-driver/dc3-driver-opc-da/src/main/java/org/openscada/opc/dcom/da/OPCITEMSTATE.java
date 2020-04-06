/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
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
}
