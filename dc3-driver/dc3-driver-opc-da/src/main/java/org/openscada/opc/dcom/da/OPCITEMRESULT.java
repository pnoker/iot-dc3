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
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIVariant;

public class OPCITEMRESULT {
    private int _serverHandle = 0;

    private short _canonicalDataType = JIVariant.VT_EMPTY;

    private short _reserved = 0;

    private int _accessRights = 0;

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

        result.setServerHandle((Integer) struct.getMember(0));
        result.setCanonicalDataType((Short) struct.getMember(1));
        result.setReserved((Short) struct.getMember(2));
        result.setAccessRights((Integer) struct.getMember(3));

        return result;
    }

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
}
