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
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.Result;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.OPCDATASOURCE;
import org.openscada.opc.dcom.da.OPCITEMSTATE;
import org.openscada.opc.dcom.da.WriteRequest;

public class OPCSyncIO extends BaseCOMObject {
    public OPCSyncIO(final IJIComObject opcSyncIO) throws JIException {
        super(opcSyncIO.queryInterface(Constants.IOPCSyncIO_IID));
    }

    public KeyedResultSet<Integer, OPCITEMSTATE> read(final OPCDATASOURCE source, final Integer... serverHandles) throws JIException {
        if (serverHandles == null || serverHandles.length == 0) {
            return new KeyedResultSet<Integer, OPCITEMSTATE>();
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsShort((short) source.id(), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(serverHandles.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(serverHandles, true), JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIArray(OPCITEMSTATE.getStruct(), null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object result[] = Helper.callRespectSFALSE(getCOMObject(), callObject);

        KeyedResultSet<Integer, OPCITEMSTATE> results = new KeyedResultSet<Integer, OPCITEMSTATE>();
        JIStruct[] states = (JIStruct[]) ((JIArray) ((JIPointer) result[0]).getReferent()).getArrayInstance();
        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[1]).getReferent()).getArrayInstance();

        for (int i = 0; i < serverHandles.length; i++) {
            results.add(new KeyedResult<Integer, OPCITEMSTATE>(serverHandles[i], OPCITEMSTATE.fromStruct(states[i]), errorCodes[i]));
        }

        return results;
    }

    public ResultSet<WriteRequest> write(final WriteRequest... requests) throws JIException {
        if (requests.length == 0) {
            return new ResultSet<WriteRequest>();
        }

        Integer[] items = new Integer[requests.length];
        JIVariant[] values = new JIVariant[requests.length];
        for (int i = 0; i < requests.length; i++) {
            items[i] = requests[i].getServerHandle();
            values[i] = Helper.fixVariant(requests[i].getValue());
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addInParamAsInt(requests.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(items, true), JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(values, true), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object result[] = Helper.callRespectSFALSE(getCOMObject(), callObject);

        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[0]).getReferent()).getArrayInstance();

        ResultSet<WriteRequest> results = new ResultSet<WriteRequest>();
        for (int i = 0; i < requests.length; i++) {
            results.add(new Result<WriteRequest>(requests[i], errorCodes[i]));
        }
        return results;
    }
}
