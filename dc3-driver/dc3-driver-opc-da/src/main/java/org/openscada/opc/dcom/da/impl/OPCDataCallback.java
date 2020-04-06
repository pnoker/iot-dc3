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
import org.openscada.opc.dcom.common.*;
import org.openscada.opc.dcom.common.impl.EventHandlerImpl;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.IOPCDataCallback;
import org.openscada.opc.dcom.da.ValueData;

import java.util.LinkedList;
import java.util.List;

public class OPCDataCallback extends EventHandlerImpl {
    private IOPCDataCallback callback = null;

    private JILocalCoClass coClass = null;

    public OPCDataCallback() {
        super();
    }

    public Object[] OnDataChange(final int transactionId, final int serverGroupHandle, final int masterQuality, final int masterErrorCode, final int count, final JIArray clientHandles, final JIArray values, final JIArray qualities, final JIArray timestamps, final JIArray errors) {
        final IOPCDataCallback callback = this.callback;
        if (callback == null) {
            return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
        }

        // get arrays for more readable code later ;-)
        final Integer[] errorCodes = (Integer[]) errors.getArrayInstance();
        final Integer[] itemHandles = (Integer[]) clientHandles.getArrayInstance();
        final Short[] qualitiesArray = (Short[]) qualities.getArrayInstance();
        final JIVariant[] valuesArray = (JIVariant[]) values.getArrayInstance();
        final JIStruct[] timestampArray = (JIStruct[]) timestamps.getArrayInstance();

        // create result data
        final KeyedResultSet<Integer, ValueData> result = new KeyedResultSet<Integer, ValueData>();
        for (int i = 0; i < count; i++) {
            final ValueData vd = new ValueData();
            vd.setQuality(qualitiesArray[i]);
            vd.setTimestamp(FILETIME.fromStruct(timestampArray[i]).asCalendar());
            vd.setValue(valuesArray[i]);
            result.add(new KeyedResult<Integer, ValueData>(itemHandles[i], vd, errorCodes[i]));
        }

        // fire event
        try {
            callback.dataChange(transactionId, serverGroupHandle, masterQuality, masterErrorCode, result);
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        // The client must always return S_OK
        return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
    }

    public synchronized Object[] OnReadComplete(final int transactionId, final int serverGroupHandle, final int masterQuality, final int masterErrorCode, final int count, final JIArray clientHandles, final JIArray values, final JIArray qualities, final JIArray timestamps, final JIArray errors) {
        if (this.callback == null) {
            return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
        }

        // get arrays for more readable code later ;-)
        final Integer[] errorCodes = (Integer[]) errors.getArrayInstance();
        final Integer[] itemHandles = (Integer[]) clientHandles.getArrayInstance();
        final Short[] qualitiesArray = (Short[]) qualities.getArrayInstance();
        final JIVariant[] valuesArray = (JIVariant[]) values.getArrayInstance();
        final JIStruct[] timestampArray = (JIStruct[]) timestamps.getArrayInstance();

        // create result data
        final KeyedResultSet<Integer, ValueData> result = new KeyedResultSet<Integer, ValueData>();
        for (int i = 0; i < count; i++) {
            final ValueData vd = new ValueData();
            vd.setQuality(qualitiesArray[i]);
            vd.setTimestamp(FILETIME.fromStruct(timestampArray[i]).asCalendar());
            vd.setValue(valuesArray[i]);
            result.add(new KeyedResult<Integer, ValueData>(itemHandles[i], vd, errorCodes[i]));
        }

        // fire event
        try {
            this.callback.readComplete(transactionId, serverGroupHandle, masterQuality, masterErrorCode, result);
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        // The client must always return S_OK
        return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
    }

    public synchronized Object[] OnWriteComplete(final int transactionId, final int serverGroupHandle, final int masterErrorCode, final int count, final JIArray clientHandles, final JIArray errors) {
        if (this.callback == null) {
            return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
        }

        // get arrays for more readable code later ;-)
        final Integer[] errorCodes = (Integer[]) errors.getArrayInstance();
        final Integer[] itemHandles = (Integer[]) clientHandles.getArrayInstance();

        // create result data
        final ResultSet<Integer> result = new ResultSet<Integer>();
        for (int i = 0; i < count; i++) {
            result.add(new Result<Integer>(itemHandles[i], errorCodes[i]));
        }

        // fire event
        try {
            this.callback.writeComplete(transactionId, serverGroupHandle, masterErrorCode, result);
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        // The client must always return S_OK
        return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
    }

    public synchronized Object[] OnCancelComplete(final int transactionId, final int serverGroupHandle) {
        if (this.callback == null) {
            return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
        }

        this.callback.cancelComplete(transactionId, serverGroupHandle);

        // The client must always return S_OK
        return new Object[]{org.openscada.opc.dcom.common.Constants.S_OK};
    }

    public synchronized JILocalCoClass getCoClass() throws JIException {
        if (this.coClass != null) {
            return this.coClass;
        }

        this.coClass = new JILocalCoClass(new JILocalInterfaceDefinition(Constants.IOPCDataCallback_IID, false), this, false);

        JILocalParamsDescriptor params;
        JILocalMethodDescriptor method;

        // OnDataChange
        params = new JILocalParamsDescriptor();
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL); // trans id
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL); // group handle
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL); // master quality
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL); // master error
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL); // count
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL); // item handles
        params.addInParamAsObject(new JIArray(JIVariant.class, null, 1, true), JIFlags.FLAG_NULL); // values
        params.addInParamAsObject(new JIArray(Short.class, null, 1, true), JIFlags.FLAG_NULL); // qualities
        params.addInParamAsObject(new JIArray(FILETIME.getStruct(), null, 1, true), JIFlags.FLAG_NULL); // timestamps
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL); // errors

        method = new JILocalMethodDescriptor("OnDataChange", params);
        this.coClass.getInterfaceDefinition().addMethodDescriptor(method);

        // OnReadComplete
        params = new JILocalParamsDescriptor();
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(JIVariant.class, null, 1, true), JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(Short.class, null, 1, true), JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(FILETIME.getStruct(), null, 1, true), JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL);
        method = new JILocalMethodDescriptor("OnReadComplete", params);
        this.coClass.getInterfaceDefinition().addMethodDescriptor(method);

        // OnWriteComplete
        params = new JILocalParamsDescriptor();
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL);
        params.addInParamAsObject(new JIArray(Integer.class, null, 1, true), JIFlags.FLAG_NULL);
        method = new JILocalMethodDescriptor("OnWriteComplete", params);
        this.coClass.getInterfaceDefinition().addMethodDescriptor(method);

        // OnCancelComplete
        params = new JILocalParamsDescriptor();
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        params.addInParamAsType(Integer.class, JIFlags.FLAG_NULL);
        method = new JILocalMethodDescriptor("OnCancelComplete", params);
        this.coClass.getInterfaceDefinition().addMethodDescriptor(method);

        // Add supported event interfaces
        final List<String> eventInterfaces = new LinkedList<String>();
        eventInterfaces.add(Constants.IOPCDataCallback_IID);
        this.coClass.setSupportedEventInterfaces(eventInterfaces);

        return this.coClass;
    }

    public void setCallback(final IOPCDataCallback callback) {
        this.callback = callback;
    }

    public IOPCDataCallback getCallback() {
        return this.callback;
    }
}
