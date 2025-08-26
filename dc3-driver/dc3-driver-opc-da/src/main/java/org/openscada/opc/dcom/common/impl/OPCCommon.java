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

    public int getLocaleID() throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addOutParamAsObject(Integer.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);
        return (Integer) result[0];
    }

    public void setLocaleID(final int localeID) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsInt(localeID, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
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
