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
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIVariant;

public class OPCITEMRESULT {
    private int _serverHandle = 0;

    private short _canonicalDataType = JIVariant.VT_EMPTY;

    private short _reserved = 0;

    private int _accessRights = 0;

    public int getAccessRights() {
        return this._accessRights;
    }

    public void setAccessRights(final int accessRights) {
        this._accessRights = accessRights;
    }

    public short getCanonicalDataType() {
        return this._canonicalDataType;
    }

    public void setCanonicalDataType(final short canonicalDataType) {
        this._canonicalDataType = canonicalDataType;
    }

    public short getReserved() {
        return this._reserved;
    }

    public void setReserved(final short reserved) {
        this._reserved = reserved;
    }

    public int getServerHandle() {
        return this._serverHandle;
    }

    public void setServerHandle(final int serverHandle) {
        this._serverHandle = serverHandle;
    }

    public static JIStruct getStruct() throws JIException {
        JIStruct struct = new JIStruct();

        struct.addMember(Integer.class); // Server handle
        struct.addMember(Short.class); // data type
        struct.addMember(Short.class); // reserved
        struct.addMember(Integer.class); // access rights
        struct.addMember(Integer.class); // blob size
        // grab the normally unused byte array
        struct.addMember(new JIPointer(new JIArray(Byte.class, null, 1, true, false)));

        return struct;
    }

    public static OPCITEMRESULT fromStruct(final JIStruct struct) {
        OPCITEMRESULT result = new OPCITEMRESULT();

        result.setServerHandle(new Integer((Integer) struct.getMember(0)));
        result.setCanonicalDataType(new Short((Short) struct.getMember(1)));
        result.setReserved(new Short((Short) struct.getMember(2)));
        result.setAccessRights(new Integer((Integer) struct.getMember(3)));

        return result;
    }
}
