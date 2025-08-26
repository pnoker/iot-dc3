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

    private final Map<Integer, String> _messageCache = new HashMap<Integer, String>();
    private OPCCommon _opcCommon = null;
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
