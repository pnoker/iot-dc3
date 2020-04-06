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

package org.openscada.opc.dcom.list.impl;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.common.impl.EnumGUID;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.dcom.list.Constants;
import rpc.core.UUID;

import java.net.UnknownHostException;

/**
 * This class implements the IOPCServerList (aka OPCEnum) service.
 *
 * @author Jens Reimann &lt;jens.reimann@th4-systems.com&gt;
 */
public class OPCServerList extends BaseCOMObject {
    public OPCServerList(final IJIComObject listObject) throws JIException {
        super(listObject.queryInterface(Constants.IOPCServerList_IID));
    }

    public JIClsid getCLSIDFromProgID(final String progId) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addInParamAsString(progId, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addOutParamAsType(UUID.class, JIFlags.FLAG_NULL);

        try {
            Object[] result = getCOMObject().call(callObject);
            return JIClsid.valueOf(((UUID) result[0]).toString());
        } catch (JIException e) {
            if (e.getErrorCode() == 0x800401F3) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Return details about a serve class
     *
     * @param clsId A server class
     * @throws JIException
     */
    public ClassDetails getClassDetails(final JIClsid clsId) throws JIException {
        if (clsId == null) {
            return null;
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addInParamAsUUID(clsId.getCLSID(), JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)), JIFlags.FLAG_NULL);

        Object[] result = Helper.callRespectSFALSE(getCOMObject(), callObject);

        ClassDetails cd = new ClassDetails();
        cd.setClsId(clsId.getCLSID());
        cd.setProgId(((JIString) ((JIPointer) result[0]).getReferent()).getString());
        cd.setDescription(((JIString) ((JIPointer) result[1]).getReferent()).getString());

        return cd;
    }

    /*
     HRESULT EnumClassesOfCategories(
     [in]                       ULONG        cImplemented,
     [in,size_is(cImplemented)] CATID        rgcatidImpl[],
     [in]                       ULONG        cRequired,
     [in,size_is(cRequired)]    CATID        rgcatidReq[],
     [out]                      IEnumGUID ** ppenumClsid
     );
     */

    public EnumGUID enumClassesOfCategories(final String[] implemented, final String[] required) throws IllegalArgumentException, UnknownHostException, JIException {
        UUID[] u1 = new UUID[implemented.length];
        UUID[] u2 = new UUID[required.length];

        for (int i = 0; i < implemented.length; i++) {
            u1[i] = new UUID(implemented[i]);
        }

        for (int i = 0; i < required.length; i++) {
            u2[i] = new UUID(required[i]);
        }

        return enumClassesOfCategories(u1, u2);
    }

    public EnumGUID enumClassesOfCategories(final UUID[] implemented, final UUID[] required) throws IllegalArgumentException, UnknownHostException, JIException {
        // ** CALL
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        // ** IN
        callObject.addInParamAsInt(implemented.length, JIFlags.FLAG_NULL);
        if (implemented.length == 0) {
            callObject.addInParamAsPointer(new JIPointer(null), JIFlags.FLAG_NULL);
        } else {
            callObject.addInParamAsArray(new JIArray(implemented, true), JIFlags.FLAG_NULL);
        }

        callObject.addInParamAsInt(required.length, JIFlags.FLAG_NULL);
        if (required.length == 0) {
            callObject.addInParamAsPointer(new JIPointer(null), JIFlags.FLAG_NULL);
        } else {
            callObject.addInParamAsArray(new JIArray(required, true), JIFlags.FLAG_NULL);
        }

        // ** OUT
        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        // ** RESULT
        Object result[] = Helper.callRespectSFALSE(getCOMObject(), callObject);

        return new EnumGUID((IJIComObject) result[0]);
    }
}
