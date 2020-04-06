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
