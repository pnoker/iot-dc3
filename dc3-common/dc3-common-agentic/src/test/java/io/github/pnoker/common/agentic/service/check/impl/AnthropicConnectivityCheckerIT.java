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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Hits the Anthropic checker against a loopback HTTP stub: asserts the wire
 * shape (x-api-key header, minimal non-streaming messages probe) and the L1
 * model-list path.
 */
class AnthropicConnectivityCheckerIT {

    private static final String MODELS = "/v1/models";
    private static final String MESSAGES = "/v1/messages";

    private LoopbackStubServer server;

    private AgenticProperties properties;

    private AnthropicConnectivityChecker checker;

    @BeforeEach
    void setUp() throws IOException {
        server = LoopbackStubServer.start(MODELS, MESSAGES);
        properties = new AgenticProperties();
        properties.setCheckL1TimeoutSeconds(5);
        properties.setCheckL2TimeoutSeconds(5);
        checker = new AnthropicConnectivityChecker(properties);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private ModelProviderBO target(String apiKey) {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setProviderType(AgenticModelProviderTypeEnum.ANTHROPIC);
        provider.setBaseUrl(server.baseUrl());
        provider.setApiKey(apiKey);
        return provider;
    }

    @Test
    void l1ListsModelsFromTheModelsEndpoint() {
        server.enqueue(
                MODELS,
                200,
                "{\"data\":[{\"id\":\"claude-1\",\"display_name\":\"C1\",\"type\":\"model\","
                        + "\"created_at\":\"2026-01-01T00:00:00Z\"}],\"has_more\":false,"
                        + "\"first_id\":\"claude-1\",\"last_id\":\"claude-1\"}");

        ConnectivityLevelResult result = checker.checkL1(target("k1")).block();

        assertEquals(ConnectivityStatusEnum.PASS, result.status());
        assertEquals(List.of("claude-1"), result.models());
    }

    @Test
    void l1ClassifiesUnauthorized() {
        server.enqueue(
                MODELS,
                401,
                "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}");

        ConnectivityLevelResult result = checker.checkL1(target("bad-key")).block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.AUTH_FAILED, result.errorType());
        assertEquals(401, result.upstreamStatus());
    }

    @Test
    void l2SendsMinimalNonStreamingMessageProbe() {
        server.enqueue(
                MESSAGES,
                200,
                "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"probe-model\","
                        + "\"content\":[{\"type\":\"text\",\"text\":\".\"}],\"stop_reason\":\"end_turn\","
                        + "\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}");

        ConnectivityLevelResult result =
                checker.checkL2(target("k1"), "probe-model").block();

        assertEquals(ConnectivityStatusEnum.PASS, result.status());
        assertEquals("probe-model", result.model());
        String body = server.lastBody(MESSAGES);
        assertTrue(body.contains("\"model\":\"probe-model\""));
        assertTrue(body.contains("\"max_tokens\":1"));
        assertTrue(body.contains("ping"));
        assertTrue(body.contains("\"user\""));
        assertFalse(body.contains("\"stream\":true"));
    }

    @Test
    void l2ClassifiesQuotaKeywordsFromRelayGateways() {
        server.enqueue(
                MESSAGES,
                402,
                "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"余额不足\"}}");

        ConnectivityLevelResult result =
                checker.checkL2(target("k1"), "probe-model").block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.QUOTA_EXCEEDED, result.errorType());
        assertEquals(402, result.upstreamStatus());
    }
}
