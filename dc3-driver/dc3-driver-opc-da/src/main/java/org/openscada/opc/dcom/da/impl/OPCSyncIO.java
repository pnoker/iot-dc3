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
