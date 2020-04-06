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

package org.openscada.opc.dcom.da.impl;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.common.impl.EnumString;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.dcom.common.impl.OPCCommon;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.OPCENUMSCOPE;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;

import java.net.UnknownHostException;

public class OPCServer extends BaseCOMObject {
    public OPCServer(final IJIComObject opcServer) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcServer.queryInterface(Constants.IOPCServer_IID));
    }

    /**
     * Retrieve the current server status
     *
     * @return the current server status
     * @throws JIException
     */
    public OPCSERVERSTATUS getStatus() throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addOutParamAsObject(new JIPointer(OPCSERVERSTATUS.getStruct()), JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        return OPCSERVERSTATUS.fromStruct((JIStruct) ((JIPointer) result[0]).getReferent());
    }

    public OPCGroupStateMgt addGroup(final String name, final boolean active, final int updateRate, final int clientHandle, final Integer timeBias, final Float percentDeadband, final int localeID) throws JIException, IllegalArgumentException, UnknownHostException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsString(name, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsInt(active ? 1 : 0, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(updateRate, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(clientHandle, JIFlags.FLAG_NULL);
        callObject.addInParamAsPointer(new JIPointer(timeBias), JIFlags.FLAG_NULL);
        callObject.addInParamAsPointer(new JIPointer(percentDeadband), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(localeID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addInParamAsUUID(Constants.IOPCGroupStateMgt_IID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        return new OPCGroupStateMgt((IJIComObject) result[2]);
    }

    public void removeGroup(final int serverHandle, final boolean force) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(4);

        callObject.addInParamAsInt(serverHandle, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(force ? 1 : 0, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }

    public void removeGroup(final OPCGroupStateMgt group, final boolean force) throws JIException {
        removeGroup(group.getState().getServerHandle(), force);
    }

    public OPCGroupStateMgt getGroupByName(final String name) throws JIException, IllegalArgumentException, UnknownHostException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addInParamAsString(name, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsUUID(Constants.IOPCGroupStateMgt_IID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        return new OPCGroupStateMgt((IJIComObject) result[0]);
    }

    /**
     * Get the groups
     *
     * @param scope The scope to get
     * @return A string enumerator with the groups
     * @throws JIException
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     */
    public EnumString getGroups(final OPCENUMSCOPE scope) throws JIException, IllegalArgumentException, UnknownHostException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(5);

        callObject.addInParamAsShort((short) scope.id(), JIFlags.FLAG_NULL);
        callObject.addInParamAsUUID(org.openscada.opc.dcom.common.Constants.IEnumString_IID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        Object[] result = Helper.callRespectSFALSE(getCOMObject(), callObject);

        return new EnumString((IJIComObject) result[0]);
    }

    public OPCItemProperties getItemPropertiesService() {
        try {
            return new OPCItemProperties(getCOMObject());
        } catch (Exception e) {
            return null;
        }
    }

    public OPCItemIO getItemIOService() {
        try {
            return new OPCItemIO(getCOMObject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the browser object (<code>IOPCBrowseServerAddressSpace</code>) from the server instance
     *
     * @return the browser object
     */
    public OPCBrowseServerAddressSpace getBrowser() {
        try {
            return new OPCBrowseServerAddressSpace(getCOMObject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the common interface if supported
     *
     * @return the common interface or <code>null</code> if it is not supported
     */
    public OPCCommon getCommon() {
        try {
            return new OPCCommon(getCOMObject());
        } catch (Exception e) {
            return null;
        }
    }
}
