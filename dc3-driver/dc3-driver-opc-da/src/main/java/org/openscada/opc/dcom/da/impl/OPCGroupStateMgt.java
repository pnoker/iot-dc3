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
import org.jinterop.dcom.impls.JIObjectFactory;
import org.openscada.opc.dcom.common.EventHandler;
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.IOPCDataCallback;
import org.openscada.opc.dcom.da.OPCGroupState;

import java.net.UnknownHostException;

/**
 * Implementation of <code>IOPCGroupStateMgt</code>
 *
 * @author Jens Reimann <jens.reimann@th4-systems.com>
 */
public class OPCGroupStateMgt extends BaseCOMObject {
    public OPCGroupStateMgt(final IJIComObject opcGroup) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcGroup.queryInterface(Constants.IOPCGroupStateMgt_IID));
    }

    public OPCGroupState getState() throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Boolean.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Float.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        final Object result[] = getCOMObject().call(callObject);

        final OPCGroupState state = new OPCGroupState();
        state.setUpdateRate((Integer) result[0]);
        state.setActive((Boolean) result[1]);
        state.setName(((JIString) ((JIPointer) result[2]).getReferent()).getString());
        state.setTimeBias((Integer) result[3]);
        state.setPercentDeadband((Float) result[4]);
        state.setLocaleID((Integer) result[5]);
        state.setClientHandle((Integer) result[6]);
        state.setServerHandle((Integer) result[7]);

        return state;
    }

    /**
     * Set the group state Leaving any of the parameters <code>null</code> will keep the current value untouched.
     *
     * @param requestedUpdateRate the requested update rate
     * @param active              Flag if the group is active or not
     * @param timeBias            The time bias
     * @param percentDeadband     the deadband percent
     * @param localeID            the locale ID
     * @param clientHandle        the client handle
     * @return the granted update rate
     * @throws JIException
     */
    public int setState(final Integer requestedUpdateRate, final Boolean active, final Integer timeBias, final Float percentDeadband, final Integer localeID, final Integer clientHandle) throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addInParamAsPointer(new JIPointer(requestedUpdateRate), JIFlags.FLAG_NULL);
        if (active != null) {
            callObject.addInParamAsPointer(new JIPointer(Integer.valueOf(active.booleanValue() ? 1 : 0)), JIFlags.FLAG_NULL);
        } else {
            callObject.addInParamAsPointer(new JIPointer(null), JIFlags.FLAG_NULL);
        }
        callObject.addInParamAsPointer(new JIPointer(timeBias), JIFlags.FLAG_NULL);
        callObject.addInParamAsPointer(new JIPointer(percentDeadband), JIFlags.FLAG_NULL);
        callObject.addInParamAsPointer(new JIPointer(localeID), JIFlags.FLAG_NULL);
        callObject.addInParamAsPointer(new JIPointer(clientHandle), JIFlags.FLAG_NULL);

        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        final Object[] result = getCOMObject().call(callObject);

        return (Integer) result[0];
    }

    public OPCItemMgt getItemManagement() throws JIException {
        return new OPCItemMgt(getCOMObject());
    }

    /**
     * Rename to group
     *
     * @param name the new name
     * @throws JIException
     */
    public void setName(final String name) throws JIException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addInParamAsString(name, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);

        getCOMObject().call(callObject);
    }

    /**
     * Clone the group
     *
     * @param name the name of the cloned group
     * @return The cloned group
     * @throws JIException
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     */
    public OPCGroupStateMgt clone(final String name) throws JIException, IllegalArgumentException, UnknownHostException {
        final JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addInParamAsString(name, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsUUID(Constants.IOPCGroupStateMgt_IID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        final Object[] result = getCOMObject().call(callObject);
        return new OPCGroupStateMgt((IJIComObject) result[0]);
    }

    /**
     * Attach a new callback to the group
     *
     * @param callback The callback to attach
     * @return The event handler information
     * @throws JIException
     */
    public EventHandler attach(final IOPCDataCallback callback) throws JIException {
        final OPCDataCallback callbackObject = new OPCDataCallback();

        callbackObject.setCallback(callback);

        // sync the callback object so that no calls get through the callback
        // until the callback information is set
        // If happens in some cases that the callback is triggered before
        // the method attachEventHandler returns.
        synchronized (callbackObject) {
            final String id = JIFrameworkHelper.attachEventHandler(getCOMObject(), Constants.IOPCDataCallback_IID, JIObjectFactory.buildObject(getCOMObject().getAssociatedSession(), callbackObject.getCoClass()));

            callbackObject.setInfo(getCOMObject(), id);
        }
        return callbackObject;
    }

    public OPCAsyncIO2 getAsyncIO2() {
        try {
            return new OPCAsyncIO2(getCOMObject());
        } catch (final Exception e) {
            return null;
        }
    }

    public OPCSyncIO getSyncIO() {
        try {
            return new OPCSyncIO(getCOMObject());
        } catch (final Exception e) {
            return null;
        }
    }
}
