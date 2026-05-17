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

package io.github.pnoker.driver.coap.server.resource;

import io.github.pnoker.driver.coap.entity.CoapMessage;
import io.github.pnoker.driver.coap.service.CoapReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * CoAP Data Resource
 * <p>
 * Handles POST requests from devices pushing telemetry data.
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
@Slf4j
public class DataResource extends CoapResource {

    private final CoapReceiveService coapReceiveService;

    public DataResource(String name, CoapReceiveService coapReceiveService) {
        super(name);
        this.coapReceiveService = coapReceiveService;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        try {
            String payload = exchange.getRequestText();
            if (payload == null || payload.isEmpty()) {
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Empty payload");
                return;
            }

            CoapMessage message = CoapMessage.builder()
                    .sourceAddress(exchange.getSourceContext().getPeerAddress().getAddress().getHostAddress())
                    .sourcePort(exchange.getSourceContext().getPeerAddress().getPort())
                    .uriPath(exchange.getRequestOptions().getUriPathString())
                    .payload(payload)
                    .contentType(String.valueOf(exchange.getRequestOptions().getContentFormat()))
                    .method("POST")
                    .build();

            log.debug("CoAP POST received from {}:{}, uri: {}, payload: {}",
                    message.getSourceAddress(), message.getSourcePort(), message.getUriPath(), payload);

            coapReceiveService.receiveValue(message);
            exchange.respond(CoAP.ResponseCode.CHANGED);
        } catch (Exception e) {
            log.error("Failed to handle CoAP POST", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.CONTENT, "CoAP Data Resource is active");
    }

}
