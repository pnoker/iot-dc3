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

package org.openscada.opc.dcom.common.impl;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.*;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

public class OPCCommon extends BaseCOMObject {
    public OPCCommon(final IJIComObject opcObject) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcObject.queryInterface(org.openscada.opc.dcom.common.Constants.IOPCCommon_IID));
    }

    public void setLocaleID(final int localeID) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsInt(localeID, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }

    public int getLocaleID() throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addOutParamAsObject(Integer.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);
        return (Integer) result[0];
    }

    public String getErrorString(final int errorCode, final int localeID) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addInParamAsInt(errorCode, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(localeID, JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)), JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);
        return ((JIString) ((JIPointer) result[0]).getReferent()).getString();
    }

    public void setClientName(final String clientName) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(4);

        callObject.addInParamAsString(clientName, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);

        getCOMObject().call(callObject);
    }

    public Collection<Integer> queryAvailableLocaleIDs() throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        JIArray resultArray = (JIArray) ((JIPointer) result[1]).getReferent();
        Integer[] intArray = (Integer[]) resultArray.getArrayInstance();

        return Arrays.asList(intArray);
    }

}
