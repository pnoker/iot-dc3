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

package io.github.pnoker.driver.coap.service;

import io.github.pnoker.driver.coap.entity.CoapMessage;

import java.util.List;

/**
 * CoAP Receive Service Interface
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
public interface CoapReceiveService {

    /**
     * Process a single CoAP message from a device.
     *
     * @param coapMessage CoapMessage
     */
    void receiveValue(CoapMessage coapMessage);

    /**
     * Process a batch of CoAP messages from devices.
     *
     * @param coapMessageList CoapMessage list
     */
    void receiveValues(List<CoapMessage> coapMessageList);

}
