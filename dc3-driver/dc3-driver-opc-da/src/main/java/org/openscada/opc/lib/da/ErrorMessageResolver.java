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
