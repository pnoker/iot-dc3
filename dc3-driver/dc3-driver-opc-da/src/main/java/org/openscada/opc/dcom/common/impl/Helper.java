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
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIVariant;

public class Helper {
    /**
     * Make the COM call but do not treat S_FALSE as error condition for the whole call
     *
     * @param object     the object to make to call on
     * @param callObject the call object
     * @return the result of the call
     * @throws JIException
     */
    public static Object[] callRespectSFALSE(final IJIComObject object, final JICallBuilder callObject) throws JIException {
        try {
            return object.call(callObject);
        } catch (JIException e) {
            if (e.getErrorCode() != org.openscada.opc.dcom.common.Constants.S_FALSE) {
                throw e;
            }
            return callObject.getResultsInCaseOfException();
        }
    }

    /**
     * Perform some fixes on the variant when writing it to OPC items. This method
     * only changes control information on the variant and not the value itself!
     *
     * @param value the value to fix
     * @return the fixed value
     * @throws JIException In case something goes wrong
     */
    public static JIVariant fixVariant(final JIVariant value) throws JIException {
        if (value.isArray()) {
            if (value.getObjectAsArray().getArrayInstance() instanceof Boolean[]) {
                value.setFlag(JIFlags.FLAG_REPRESENTATION_VARIANT_BOOL);
            }
        }
        return value;
    }
}
