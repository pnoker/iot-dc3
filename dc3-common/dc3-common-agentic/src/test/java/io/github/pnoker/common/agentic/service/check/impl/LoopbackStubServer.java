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
package io.github.pnoker.common.agentic.service.check.impl;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Loopback HTTP stub shared by the connectivity checker ITs: per-path response
 * queues plus captured Authorization headers and request bodies.
 */
final class LoopbackStubServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Queue<StubResponse>> responses = new HashMap<>();
    private final Map<String, List<String>> bodies = new HashMap<>();
    private final Map<String, List<String>> authorizations = new HashMap<>();

    private LoopbackStubServer(HttpServer server) {
        this.server = server;
    }

    static LoopbackStubServer start(String... paths) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        LoopbackStubServer stub = new LoopbackStubServer(httpServer);
        for (String path : paths) {
            stub.responses.put(path, new ArrayDeque<>());
            stub.bodies.put(path, new ArrayList<>());
            stub.authorizations.put(path, new ArrayList<>());
            httpServer.createContext(path, exchange -> {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                stub.bodies.get(path).add(requestBody);
                stub.authorizations.get(path).add(exchange.getRequestHeaders().getFirst("Authorization"));
                StubResponse response = stub.responses.get(path).poll();
                if (response == null) {
                    byte[] missing =
                            "{\"error\":{\"message\":\"no stub response queued\"}}".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, missing.length);
                    exchange.getResponseBody().write(missing);
                    exchange.close();
                    return;
                }
                if (response.delayMillis() > 0) {
                    try {
                        Thread.sleep(response.delayMillis());
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
        }
        httpServer.start();
        return stub;
    }

    void enqueue(String path, int status, String body) {
        responses.get(path).add(new StubResponse(status, body, 0));
    }

    void enqueueDelayed(String path, long delayMillis) {
        responses.get(path).add(new StubResponse(200, "{}", delayMillis));
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String lastBody(String path) {
        List<String> captured = bodies.get(path);
        return captured.isEmpty() ? "" : captured.get(captured.size() - 1);
    }

    String lastBody(int index) {
        for (List<String> captured : bodies.values()) {
            if (captured.size() > index) return captured.get(index);
        }
        return "";
    }

    String lastAuthorization(String path) {
        List<String> captured = authorizations.get(path);
        return captured.isEmpty() ? null : captured.get(captured.size() - 1);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record StubResponse(int status, String body, long delayMillis) {}
}
