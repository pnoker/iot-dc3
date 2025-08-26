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

package com.serotonin.modbus4j.sero.messaging;


/**
 * <p>RequestHandler interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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
