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
