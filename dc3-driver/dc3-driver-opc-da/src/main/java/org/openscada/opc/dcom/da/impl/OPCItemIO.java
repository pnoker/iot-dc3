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
import org.openscada.opc.dcom.common.FILETIME;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.IORequest;

import java.net.UnknownHostException;

public class OPCItemIO extends BaseCOMObject {
    public OPCItemIO(final IJIComObject opcItemIO) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcItemIO.queryInterface(Constants.IOPCItemIO_IID));
    }

    public void read(final IORequest[] requests) throws JIException {
        if (requests.length == 0) {
            return;
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        JIString itemIDs[] = new JIString[requests.length];
        Integer maxAges[] = new Integer[requests.length];
        for (int i = 0; i < requests.length; i++) {
            itemIDs[i] = new JIString(requests[i].getItemID(), JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
            maxAges[i] = new Integer(requests[i].getMaxAge());
        }

        callObject.addInParamAsInt(requests.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(itemIDs, true), JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(maxAges, true), JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIArray(JIVariant.class, null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(FILETIME.getStruct(), null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }
}
