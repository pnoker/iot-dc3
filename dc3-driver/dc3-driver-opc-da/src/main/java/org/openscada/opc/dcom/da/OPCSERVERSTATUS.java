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
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIStruct;
import org.openscada.opc.dcom.common.FILETIME;

public class OPCSERVERSTATUS {
    private FILETIME _startTime = null;

    private FILETIME _currentTime = null;

    private FILETIME _lastUpdateTime = null;

    private OPCSERVERSTATE _serverState = null;

    private int _groupCount = -1;

    private int _bandWidth = -1;

    private short _majorVersion = -1;

    private short _minorVersion = -1;

    private short _buildNumber = -1;

    private short _reserved = 0;

    private String _vendorInfo = null;

    public static JIStruct getStruct() throws JIException {
        JIStruct struct = new JIStruct();

        struct.addMember(FILETIME.getStruct());
        struct.addMember(FILETIME.getStruct());
        struct.addMember(FILETIME.getStruct());
        struct.addMember(Short.class); // enum: OPCSERVERSTATE
        struct.addMember(Integer.class);
        struct.addMember(Integer.class);
        struct.addMember(Short.class);
        struct.addMember(Short.class);
        struct.addMember(Short.class);
        struct.addMember(Short.class);
        struct.addMember(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)));

        return struct;
    }

    public static OPCSERVERSTATUS fromStruct(final JIStruct struct) {
        OPCSERVERSTATUS status = new OPCSERVERSTATUS();

        status._startTime = FILETIME.fromStruct((JIStruct) struct.getMember(0));
        status._currentTime = FILETIME.fromStruct((JIStruct) struct.getMember(1));
        status._lastUpdateTime = FILETIME.fromStruct((JIStruct) struct.getMember(2));

        status._serverState = OPCSERVERSTATE.fromID((Short) struct.getMember(3));
        status._groupCount = (Integer) struct.getMember(4);
        status._bandWidth = (Integer) struct.getMember(5);
        status._majorVersion = (Short) struct.getMember(6);
        status._minorVersion = (Short) struct.getMember(7);
        status._buildNumber = (Short) struct.getMember(8);
        status._reserved = (Short) struct.getMember(9);
        status._vendorInfo = ((JIString) ((JIPointer) struct.getMember(10)).getReferent()).getString();

        return status;
    }

    public int getBandWidth() {
        return this._bandWidth;
    }

    public void setBandWidth(final int bandWidth) {
        this._bandWidth = bandWidth;
    }

    public short getBuildNumber() {
        return this._buildNumber;
    }

    public void setBuildNumber(final short buildNumber) {
        this._buildNumber = buildNumber;
    }

    public FILETIME getCurrentTime() {
        return this._currentTime;
    }

    public void setCurrentTime(final FILETIME currentTime) {
        this._currentTime = currentTime;
    }

    public int getGroupCount() {
        return this._groupCount;
    }

    public void setGroupCount(final int groupCount) {
        this._groupCount = groupCount;
    }

    public FILETIME getLastUpdateTime() {
        return this._lastUpdateTime;
    }

    public void setLastUpdateTime(final FILETIME lastUpdateTime) {
        this._lastUpdateTime = lastUpdateTime;
    }

    public short getMajorVersion() {
        return this._majorVersion;
    }

    public void setMajorVersion(final short majorVersion) {
        this._majorVersion = majorVersion;
    }

    public short getMinorVersion() {
        return this._minorVersion;
    }

    public void setMinorVersion(final short minorVersion) {
        this._minorVersion = minorVersion;
    }

    public short getReserved() {
        return this._reserved;
    }

    public void setReserved(final short reserved) {
        this._reserved = reserved;
    }

    public FILETIME getStartTime() {
        return this._startTime;
    }

    public void setStartTime(final FILETIME startTime) {
        this._startTime = startTime;
    }

    public String getVendorInfo() {
        return this._vendorInfo;
    }

    public void setVendorInfo(final String vendorInfo) {
        this._vendorInfo = vendorInfo;
    }

    public OPCSERVERSTATE getServerState() {
        return this._serverState;
    }

    public void setServerState(final OPCSERVERSTATE dwServerState) {
        this._serverState = dwServerState;
    }
}
