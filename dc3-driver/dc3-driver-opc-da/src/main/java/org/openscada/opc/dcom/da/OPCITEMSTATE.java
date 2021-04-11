/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
