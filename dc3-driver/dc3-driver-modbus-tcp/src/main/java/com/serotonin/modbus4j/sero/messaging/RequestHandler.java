/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.serotonin.modbus4j.sero.messaging;


/**
 * <p>RequestHandler interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public interface RequestHandler {
    /**
     * Handle the request and return the appropriate response object.
     *
     * @param request the request to handle
     * @return the response object or null if no response is to be sent. null may also be returned if the request is
     * handled asynchronously.
     * @throws Exception if necessary
     */
    OutgoingResponseMessage handleRequest(IncomingRequestMessage request) throws Exception;
}
