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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EnumString extends BaseCOMObject {
    public static final int DEFAULT_BATCH_SIZE = Integer.getInteger("openscada.dcom.enum-batch-size", 10);

    public EnumString(final IJIComObject enumStringObject) throws IllegalArgumentException, UnknownHostException, JIException {
        super(enumStringObject.queryInterface(org.openscada.opc.dcom.common.Constants.IEnumString_IID));
    }

    public int next(final List<String> list, final int num) throws JIException {
        if (num <= 0) {
            return 0;
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsInt(num, JIFlags.FLAG_NULL);
        //callObject.addInParamAsInt ( num, JIFlags.FLAG_NULL );
        //callObject.addOutParamAsObject ( new JIArray ( new JIPointer ( new JIString (
        //        JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR ) ), null, 1, true, true ), JIFlags.FLAG_NULL );
        callObject.addOutParamAsObject(new JIArray(new JIString(JIFlags.FLAG_REPRESENTATION_STRING_LPWSTR), null, 1, true, true), JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        Object[] result = Helper.callRespectSFALSE(getCOMObject(), callObject);

        //JIPointer[] resultData = (JIPointer[]) ( (JIArray) ( result[0] ) ).getArrayInstance ();
        JIString[] resultData = (JIString[]) ((JIArray) result[0]).getArrayInstance();
        Integer cnt = (Integer) result[1];

        for (int i = 0; i < cnt; i++) {
            //list.add ( ( (JIString)resultData[i].getReferent () ).getString () );
            list.add(resultData[i].getString());
        }
        return cnt;
    }

    public Collection<String> next(final int num) throws JIException {
        List<String> list = new ArrayList<String>(num);
        next(list, num);
        return list;
    }

    public void skip(final int num) throws JIException {
        if (num <= 0) {
            return;
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(1);

        callObject.addInParamAsInt(num, JIFlags.FLAG_NULL);

        getCOMObject().call(callObject);
    }

    public void reset() throws JIException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(2);

        getCOMObject().call(callObject);
    }

    public EnumString cloneObject() throws JIException, IllegalArgumentException, UnknownHostException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        IJIComObject object = (IJIComObject) result[0];
        return new EnumString(object);
    }

    public Collection<String> asCollection(final int batchSize) throws JIException {
        reset();

        List<String> data = new ArrayList<String>();
        int i = 0;
        do {
            i = next(data, batchSize);
        } while (i == batchSize);

        return data;
    }

    public Collection<String> asCollection() throws JIException {
        return asCollection(DEFAULT_BATCH_SIZE);
    }

}
