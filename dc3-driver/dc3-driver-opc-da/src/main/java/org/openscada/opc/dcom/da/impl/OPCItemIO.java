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
