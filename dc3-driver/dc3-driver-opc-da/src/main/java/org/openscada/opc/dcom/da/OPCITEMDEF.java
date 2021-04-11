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
import org.jinterop.dcom.core.*;

public class OPCITEMDEF {
    private String accessPath = "";

    private String itemID = "";

    private boolean active = true;

    private int clientHandle;

    private short requestedDataType = JIVariant.VT_EMPTY;

    private short reserved;

    public String getAccessPath() {
        return this.accessPath;
    }

    public void setAccessPath(final String accessPath) {
        this.accessPath = accessPath;
    }

    public int getClientHandle() {
        return this.clientHandle;
    }

    public void setClientHandle(final int clientHandle) {
        this.clientHandle = clientHandle;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getItemID() {
        return this.itemID;
    }

    public void setItemID(final String itemID) {
        this.itemID = itemID;
    }

    public short getRequestedDataType() {
        return this.requestedDataType;
    }

    public void setRequestedDataType(final short requestedDataType) {
        this.requestedDataType = requestedDataType;
    }

    public short getReserved() {
        return this.reserved;
    }

    public void setReserved(final short reserved) {
        this.reserved = reserved;
    }

    /**
     * Convert to structure to a J-Interop structure
     *
     * @return the j-interop structe
     * @throws JIException
     */
    public JIStruct toStruct() throws JIException {
        final JIStruct struct = new JIStruct();
        struct.addMember(new JIString(getAccessPath(), JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR));
        struct.addMember(new JIString(getItemID(), JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR));
        struct.addMember(new Integer(isActive() ? 1 : 0));
        struct.addMember(Integer.valueOf(getClientHandle()));

        struct.addMember(Integer.valueOf(0)); // blob size
        struct.addMember(new JIPointer(null)); // blob

        struct.addMember(Short.valueOf(getRequestedDataType()));
        struct.addMember(Short.valueOf(getReserved()));
        return struct;
    }
}
