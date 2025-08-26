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
            maxAges[i] = requests[i].getMaxAge();
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
