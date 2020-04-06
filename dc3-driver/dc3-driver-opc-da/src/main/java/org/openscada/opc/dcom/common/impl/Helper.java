/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
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
