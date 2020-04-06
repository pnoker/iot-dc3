/*
 * This file is part of the OpenSCADA project
 *
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 * Copyright (C) 2013 Jens Reimann (ctron@dentrassi.de)
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
import org.openscada.opc.dcom.common.Result;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.OPCDATASOURCE;

import java.net.UnknownHostException;

public class OPCAsyncIO2 extends BaseCOMObject {
    public class AsyncResult {
        private final ResultSet<Integer> result;

        private final Integer cancelId;

        public AsyncResult() {
            super();
            this.result = new ResultSet<Integer>();
            this.cancelId = null;
        }

        public AsyncResult(final ResultSet<Integer> result, final Integer cancelId) {
            super();
            this.result = result;
            this.cancelId = cancelId;
        }

        public Integer getCancelId() {
            return this.cancelId;
        }

        public ResultSet<Integer> getResult() {
            return this.result;
        }
    }

    public OPCAsyncIO2(final IJIComObject opcAsyncIO2) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcAsyncIO2.queryInterface(Constants.IOPCAsyncIO2_IID));
    }

    public void setEnable(final boolean state) throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(4);

        callObject.addInParamAsInt(state ? 1 : 0, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }

    public int refresh(final OPCDATASOURCE dataSource, final int transactionID) throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addInParamAsShort((short) dataSource.id(), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(transactionID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        final Object result[] = getCOMObject().call(callObject);

        return (Integer) result[0];
    }

    public void cancel(final int cancelId) throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addInParamAsInt(cancelId, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }

    public AsyncResult read(final int transactionId, final Integer... serverHandles) throws JIException {
        if (serverHandles == null || serverHandles.length == 0) {
            return new AsyncResult();
        }

        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsInt(serverHandles.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(serverHandles, true), JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(transactionId, JIFlags.FLAG_NULL);

        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        final Object[] result = getCOMObject().call(callObject);

        final Integer cancelId = (Integer) result[0];
        final Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[1]).getReferent()).getArrayInstance();

        final ResultSet<Integer> resultSet = new ResultSet<Integer>();

        for (int i = 0; i < serverHandles.length; i++) {
            resultSet.add(new Result<Integer>(serverHandles[i], errorCodes[i]));
        }

        return new AsyncResult(resultSet, cancelId);
    }
}
