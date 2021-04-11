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
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIFlags;
import rpc.core.UUID;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EnumGUID extends BaseCOMObject {
    public static final int DEFAULT_BATCH_SIZE = Integer.getInteger("openscada.dcom.enum-batch-size", 10);

    public EnumGUID(final IJIComObject enumStringObject) throws IllegalArgumentException, UnknownHostException, JIException {
        super(enumStringObject.queryInterface(org.openscada.opc.dcom.common.Constants.IEnumGUID_IID));
    }

    public int next(final List<UUID> list, final int num) throws JIException {
        if (num <= 0) {
            return 0;
        }

        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(0);

        callObject.addInParamAsInt(num, JIFlags.FLAG_NULL);
        callObject.addInParamAsInt(num, JIFlags.FLAG_NULL);
        callObject.addOutParamAsObject(new JIArray(UUID.class, null, 1, true, true), JIFlags.FLAG_NULL);
        callObject.addOutParamAsType(Integer.class, JIFlags.FLAG_NULL);

        Object[] result = Helper.callRespectSFALSE(getCOMObject(), callObject);

        UUID[] resultData = (UUID[]) ((JIArray) result[0]).getArrayInstance();
        Integer cnt = (Integer) result[1];

        for (int i = 0; i < cnt; i++) {
            list.add(resultData[i]);
        }
        return cnt;
    }

    public Collection<UUID> next(final int num) throws JIException {
        List<UUID> list = new ArrayList<UUID>(num);
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

    public EnumGUID cloneObject() throws JIException, IllegalArgumentException, UnknownHostException {
        JICallBuilder callObject = new JICallBuilder(true);
        callObject.setOpnum(3);

        callObject.addOutParamAsType(IJIComObject.class, JIFlags.FLAG_NULL);

        Object[] result = getCOMObject().call(callObject);

        IJIComObject object = (IJIComObject) result[0];

        return new EnumGUID(object);
    }

    public Collection<UUID> asCollection(final int batchSize) throws JIException {
        reset();

        List<UUID> data = new ArrayList<UUID>();
        int i = 0;
        do {
            i = next(data, batchSize);
        } while (i == batchSize);

        return data;
    }

    public Collection<UUID> asCollection() throws JIException {
        return asCollection(DEFAULT_BATCH_SIZE);
    }

}
