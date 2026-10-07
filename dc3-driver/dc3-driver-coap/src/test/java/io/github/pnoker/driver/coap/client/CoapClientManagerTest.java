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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.driver.coap.entity.CoapResult;
import io.github.pnoker.driver.coap.entity.property.CoapProperties;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.CoapExchange;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Guard tests for {@link CoapClientManager}: every pooled client must carry a started UDP
 * endpoint, and the read/write paths must exchange datagrams end to end against an in-JVM
 * Californium server. These guard against the former defect where clients were created
 * endpoint-less and every request failed before any packet was sent.
 */
class CoapClientManagerTest {

    private static final String GET_PAYLOAD = "23.5";

    private final AtomicReference<String> lastPutPayload = new AtomicReference<>();

    private final CoapProperties coapProperties = new CoapProperties();
    private final CoapClientManager manager = new CoapClientManager(coapProperties);

    private CoapServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.destroy();
        }
        manager.destroy();
    }

    @Test
    void createdClientCarriesStartedEndpoint() {
        Endpoint endpoint = manager.getClient("coap://127.0.0.1:5683").getEndpoint();

        assertThat(endpoint).isNotNull();
        assertThat(endpoint.isStarted()).isTrue();
    }

    @Test
    void getReturnsResourceServedByInJvmServer() {
        int port = startServer();

        CoapResult result = manager.get("coap://127.0.0.1:" + port, "/sensor");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(CoAP.ResponseCode.CONTENT.value);
        assertThat(result.getPayload()).isEqualTo(GET_PAYLOAD);
    }

    @Test
    void putDeliversPayloadToInJvmServer() {
        int port = startServer();

        CoapResult result = manager.put("coap://127.0.0.1:" + port, "/sensor", "ON");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(CoAP.ResponseCode.CHANGED.value);
        assertThat(lastPutPayload.get()).isEqualTo("ON");
    }

    /**
     * Start a CoAP server on an ephemeral localhost port serving a single resource that
     * answers GET with a fixed value and records PUT payloads. The server is constructed
     * with an explicit {@link Configuration} because the no-argument constructor falls back
     * to {@link Configuration#getStandard()}, which reads or writes a Californium3.properties
     * file in the working directory.
     *
     * @return the bound UDP port
     */
    private int startServer() {
        server = new CoapServer(new Configuration());
        server.add(new CoapResource("sensor") {

            @Override
            public void handleGET(CoapExchange exchange) {
                exchange.respond(GET_PAYLOAD);
            }

            @Override
            public void handlePUT(CoapExchange exchange) {
                lastPutPayload.set(exchange.getRequestText());
                exchange.respond(CoAP.ResponseCode.CHANGED);
            }
        });
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setInetSocketAddress(new InetSocketAddress(0));
        builder.setConfiguration(new Configuration());
        CoapEndpoint endpoint = builder.build();
        server.addEndpoint(endpoint);
        server.start();
        return endpoint.getAddress().getPort();
    }
}
