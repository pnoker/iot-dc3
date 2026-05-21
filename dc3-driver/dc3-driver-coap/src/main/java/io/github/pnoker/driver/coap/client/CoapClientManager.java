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

package io.github.pnoker.driver.coap.client;

import io.github.pnoker.driver.coap.entity.CoapResult;
import io.github.pnoker.driver.coap.entity.property.CoapProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * CoAP Client Manager
 * <p>
 * Manages a pool of CoapClient instances keyed by device URI.
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoapClientManager implements DisposableBean {

    private final CoapProperties coapProperties;
    private final ConcurrentHashMap<String, CoapClient> clientPool = new ConcurrentHashMap<>();

    /**
     * Get or create a CoapClient for the given URI.
     *
     * @param uri CoAP device URI (e.g., coap://host:port)
     * @return CoapClient instance
     */
    public CoapClient getClient(String uri) {
        return clientPool.computeIfAbsent(uri, this::createClient);
    }

    /**
     * Perform a GET request to the given URI + path.
     *
     * @param uri  device URI (e.g., coap://192.168.1.10:5683)
     * @param path resource path (e.g., /sensors/temperature)
     * @return CoapResult or null on failure
     */
    public CoapResult get(String uri, String path) {
        CoapClient client = getClient(uri);
        client.setURI(uri + path);
        try {
            Configuration config = client.getEndpoint().getConfig();
            config.set(CoapConfig.EXCHANGE_LIFETIME, coapProperties.getClientTimeout(), TimeUnit.MILLISECONDS);
            config.set(CoapConfig.ACK_TIMEOUT, coapProperties.getClientAckTimeout(), TimeUnit.MILLISECONDS);
            config.set(CoapConfig.MAX_RETRANSMIT, coapProperties.getClientMaxRetransmit());

            org.eclipse.californium.core.CoapResponse response = client.get();
            if (response == null) {
                log.warn("CoAP GET timeout: {}{}", uri, path);
                return null;
            }
            return toResult(response);
        } catch (Exception e) {
            log.error("CoAP GET failed: {}{}", uri, path, e);
            return null;
        }
    }

    /**
     * Perform a PUT request to the given URI + path.
     *
     * @param uri     device URI
     * @param path    resource path
     * @param payload payload to send
     * @return CoapResult or null on failure
     */
    public CoapResult put(String uri, String path, String payload) {
        CoapClient client = getClient(uri);
        client.setURI(uri + path);
        try {
            org.eclipse.californium.core.CoapResponse response = client.put(payload, MediaTypeRegistry.APPLICATION_JSON);
            if (response == null) {
                log.warn("CoAP PUT timeout: {}{}", uri, path);
                return null;
            }
            return toResult(response);
        } catch (Exception e) {
            log.error("CoAP PUT failed: {}{}", uri, path, e);
            return null;
        }
    }

    /**
     * Release the client for the given URI.
     *
     * @param uri device URI
     */
    public void releaseClient(String uri) {
        CoapClient client = clientPool.remove(uri);
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Shut down all managed clients on Spring context close.
     */
    @Override
    public void destroy() {
        clientPool.forEach((uri, client) -> client.shutdown());
        clientPool.clear();
    }

    private CoapClient createClient(String uri) {
        CoapClient client = new CoapClient(uri);
        log.debug("Created CoAP client for: {}", uri);
        return client;
    }

    private CoapResult toResult(org.eclipse.californium.core.CoapResponse response) {
        return CoapResult.builder()
                .statusCode(response.getCode().value)
                .payload(response.getResponseText())
                .contentType(String.valueOf(response.getOptions().getContentFormat()))
                .success(response.isSuccess())
                .build();
    }

}
