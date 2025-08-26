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
import org.openscada.opc.dcom.common.impl.BaseCOMObject;
import org.openscada.opc.dcom.common.impl.Helper;
import org.openscada.opc.dcom.da.Constants;
import org.openscada.opc.dcom.da.PropertyDescription;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class OPCItemProperties extends BaseCOMObject {
    public OPCItemProperties(final IJIComObject opcItemProperties) throws IllegalArgumentException, UnknownHostException, JIException {
        super(opcItemProperties.queryInterface(Constants.IOPCItemProperties_IID));
    }

    public Collection<PropertyDescription> queryAvailableProperties(final String itemID) throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsString(itemID, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);

        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_BSTR), null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Short.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object result[] = getCOMObject().call(callObject);

        List<PropertyDescription> properties = new LinkedList<PropertyDescription>();

        int len = (Integer) result[0];
        Integer[] ids = (Integer[]) ((JIArray) ((JIPointer) result[1]).getReferent()).getArrayInstance();
        JIString[] descriptions = (JIString[]) ((JIArray) ((JIPointer) result[2]).getReferent()).getArrayInstance();
        Short[] variableTypes = (Short[]) ((JIArray) ((JIPointer) result[3]).getReferent()).getArrayInstance();

        for (int i = 0; i < len; i++) {
            PropertyDescription pd = new PropertyDescription();
            pd.setId(ids[i]);
            pd.setDescription(descriptions[i].getString());
            pd.setVarType(variableTypes[i]);
            properties.add(pd);
        }
        return properties;
    }

    public KeyedResultSet<Integer, JIVariant> getItemProperties(final String itemID, final int... properties) throws JIException {
        if (properties.length == 0) {
            return new KeyedResultSet<Integer, JIVariant>();
        }

        Integer[] ids = new Integer[properties.length];
        for (int i = 0; i < properties.length; i++) {
            ids[i] = properties[i];
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addInParamAsString(itemID, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsInt(properties.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(ids, true), JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIArray(JIVariant.class, null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object result[] = Helper.callRespectSFALSE(getCOMObject(), callObject);

        JIVariant[] values = (JIVariant[]) ((JIArray) ((JIPointer) result[0]).getReferent()).getArrayInstance();
        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[1]).getReferent()).getArrayInstance();

        KeyedResultSet<Integer, JIVariant> results = new KeyedResultSet<Integer, JIVariant>();
        for (int i = 0; i < properties.length; i++) {
            results.add(new KeyedResult<Integer, JIVariant>(properties[i], values[i], errorCodes[i]));
        }
        return results;
    }

    public KeyedResultSet<Integer, String> lookupItemIDs(final String itemID, final int... properties) throws JIException {
        if (properties.length == 0) {
            return new KeyedResultSet<Integer, String>();
        }

        Integer[] ids = new Integer[properties.length];
        for (int i = 0; i < properties.length; i++) {
            ids[i] = properties[i];
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        callObject.addInParamAsString(itemID, JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR);
        callObject.addInParamAsInt(properties.length, JIFlags.FLAG_NULL);
        callObject.addInParamAsArray(new JIArray(ids, true), JIFlags.FLAG_NULL);

        callObject.addOutParamAsObject(new JIPointer(new JIArray(new JIPointer(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR)), null, 1, true)), JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIPointer(new JIArray(Integer.class, null, 1, true)), JIFlags.FLAG_NULL);

        Object result[] = Helper.callRespectSFALSE(getCOMObject(), callObject);

        JIPointer[] itemIDs = (JIPointer[]) ((JIArray) ((JIPointer) result[0]).getReferent()).getArrayInstance();
        Integer[] errorCodes = (Integer[]) ((JIArray) ((JIPointer) result[1]).getReferent()).getArrayInstance();

        KeyedResultSet<Integer, String> results = new KeyedResultSet<Integer, String>();

        for (int i = 0; i < properties.length; i++) {
            results.add(new KeyedResult<Integer, String>(properties[i], ((JIString) itemIDs[i].getReferent()).getString(), errorCodes[i]));
        }
        return results;
    }
}
