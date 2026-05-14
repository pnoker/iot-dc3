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

package io.github.pnoker.e2e;

import io.github.pnoker.e2e.harness.BaseE2eIT;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Pins the rest-assured wiring shape that the platform-driven E2E flows will use:
 *  - JSON request and response shaping
 *  - status code + body matchers
 *  - {@link io.restassured.config.RestAssuredConfig} default content type override so
 *    non-JSON content types do not silently parse as form
 *
 * The platform-image-driven user-flow scenarios (login, driver registration, write
 * point value, gateway 401, cross-tenant isolation, OFFLINE auto-status, command
 * round-trip, DLX requeue, agentic via WireMock, CSV import, token rotation) will
 * extend this harness once dc3-center-* and dc3-driver-* publish immutable image
 * tags suitable for CI. Until then the dependency on rest-assured is locked here.
 *
 * Disabled by default; opt in with {@code DC3_E2E=true}.
 */
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class RestAssuredHarnessIT extends BaseE2eIT {

    @Test
    void restAssuredCanCallAStubHttpServerAndDecodeJson() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/probe", new JsonHandler(200,
                "{\"ok\":true,\"data\":{\"name\":\"dc3-center-auth\"},\"message\":\"alive\"}"));
        server.createContext("/v3/error", new JsonHandler(401,
                "{\"ok\":false,\"data\":null,\"message\":\"Unauthorized\"}"));
        server.start();
        try {
            int port = server.getAddress().getPort();
            given()
                    .baseUri("http://127.0.0.1")
                    .port(port)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/v3/probe")
                    .then()
                    .statusCode(200)
                    .body("ok", equalTo(true))
                    .body("data.name", equalTo("dc3-center-auth"))
                    .body("message", equalTo("alive"));

            given()
                    .baseUri("http://127.0.0.1")
                    .port(port)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/v3/error")
                    .then()
                    .statusCode(401)
                    .body("ok", equalTo(false))
                    .body("message", equalTo("Unauthorized"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restAssuredHonoursRequestBodyAndCustomHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/echo", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String token = exchange.getRequestHeaders().getFirst("X-DC3-Token");
            String reply = "{\"echo\":" + new String(body, StandardCharsets.UTF_8) + ",\"token\":\""
                    + (token == null ? "" : token) + "\"}";
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            given()
                    .baseUri("http://127.0.0.1")
                    .port(port)
                    .header("X-DC3-Token", "alice-token")
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"alice\"}")
                    .when()
                    .post("/v3/echo")
                    .then()
                    .statusCode(200)
                    .body("echo.name", equalTo("alice"))
                    .body("token", equalTo("alice-token"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restAssuredConfigCanForceJsonParserForUnknownContentType() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/no-content-type", exchange -> {
            byte[] bytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            RestAssuredConfig config = RestAssured.config()
                    .encoderConfig(RestAssured.config().getEncoderConfig().defaultContentCharset("UTF-8"));
            given()
                    .config(config)
                    .baseUri("http://127.0.0.1")
                    .port(port)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/v3/no-content-type")
                    .then()
                    .statusCode(200)
                    .using()
                    .defaultParser(Parser.JSON)
                    .body("ok", notNullValue());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void responseBodyCanBeAssertedAsRawString() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/raw", new JsonHandler(200, "{\"value\":\"raw-body\"}"));
        server.start();
        try {
            int port = server.getAddress().getPort();
            String body = given()
                    .baseUri("http://127.0.0.1")
                    .port(port)
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/v3/raw")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();
            assertThat(body).contains("raw-body");
        } finally {
            server.stop(0);
        }
    }

    private static final class JsonHandler implements com.sun.net.httpserver.HttpHandler {

        private final int status;

        private final byte[] body;

        JsonHandler(int status, String body) {
            this.status = status;
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
