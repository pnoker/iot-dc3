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

package org.openscada.opc.lib.da;

import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.common.impl.OPCCommon;

import java.util.HashMap;
import java.util.Map;

/**
 * An error message resolver that will lookup the error code using the
 * server interface and will cache the result locally.
 *
 * @author Jens Reimann
 */
@Slf4j
public class ErrorMessageResolver {

    private OPCCommon _opcCommon = null;

    private final Map<Integer, String> _messageCache = new HashMap<Integer, String>();

    private int _localeId = 0;

    public ErrorMessageResolver(final OPCCommon opcCommon, final int localeId) {
        super();
        this._opcCommon = opcCommon;
        this._localeId = localeId;
    }

    /**
     * Get an error message from an error code
     *
     * @param errorCode The error code to look up
     * @return the error message or <code>null</code> if no message could be looked up
     */
    public synchronized String getMessage(final int errorCode) {
        String message = this._messageCache.get(Integer.valueOf(errorCode));

        if (message == null) {
            try {
                message = this._opcCommon.getErrorString(errorCode, this._localeId);
                log.info(String.format("Resolved %08X to '%s'", errorCode, message));
            } catch (JIException e) {
                log.warn(String.format("Failed to resolve error code for %08X", errorCode), e);
            }
            if (message != null) {
                this._messageCache.put(errorCode, message);
            }
        }
        return message;
    }
}
