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
 * Hits the OpenAI-compatible checker against a loopback HTTP stub: asserts
 * the wire shape (explicit bearer, minimal non-streaming probe) and the error
 * classification matrix.
 */
class OpenAiCompatibleConnectivityCheckerIT {

    private static final String MODELS = "/v1/models";
    private static final String CHAT = "/v1/chat/completions";

    private LoopbackStubServer server;

    private AgenticProperties properties;

    private OpenAiCompatibleConnectivityChecker checker;

    @BeforeEach
    void setUp() throws IOException {
        server = LoopbackStubServer.start(MODELS, CHAT);
        properties = new AgenticProperties();
        properties.setCheckL1TimeoutSeconds(5);
        properties.setCheckL2TimeoutSeconds(5);
        checker = new OpenAiCompatibleConnectivityChecker(properties);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private ModelProviderBO target(String apiKey) {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setProviderType(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        // Trailing slash on purpose: the checker must normalize it away.
        provider.setBaseUrl(server.baseUrl() + "/v1/");
        provider.setApiKey(apiKey);
        return provider;
    }

    @Test
    void l1ListsModelsAndSendsExplicitBearer() {
        server.enqueue(MODELS, 200, "{\"object\":\"list\",\"data\":[{\"id\":\"m1\"},{\"id\":\"m2\"}]}");

        ConnectivityLevelResult result = checker.checkL1(target("k1")).block();

        assertEquals(ConnectivityStatusEnum.PASS, result.status());
        assertEquals(List.of("m1", "m2"), result.models());
        assertTrue(result.latencyMs() >= 0);
        assertEquals("Bearer k1", server.lastAuthorization(MODELS));
    }

    @Test
    void l1TruncatesModelListToConfiguredCap() {
        properties.setCheckMaxModels(2);
        server.enqueue(
                MODELS,
                200,
                "{\"object\":\"list\",\"data\":[{\"id\":\"m1\"},{\"id\":\"m2\"},{\"id\":\"m3\"},{\"id\":\"m4\"},{\"id\":\"m5\"}]}");

        ConnectivityLevelResult result = checker.checkL1(target("k1")).block();

        assertEquals(List.of("m1", "m2"), result.models());
    }

    @Test
    void l1WithEmptyKeySendsPlaceholderBearerNotEnvCredential() {
        server.enqueue(MODELS, 200, "{\"object\":\"list\",\"data\":[]}");

        ConnectivityLevelResult result = checker.checkL1(target("")).block();

        assertEquals(ConnectivityStatusEnum.PASS, result.status());
        // The explicit placeholder proves no OPENAI_API_KEY env fallback ever
        // leaks into the probe.
        assertEquals("Bearer no-key-configured", server.lastAuthorization(MODELS));
    }

    @Test
    void l1ClassifiesUnauthorized() {
        server.enqueue(MODELS, 401, "{\"error\":{\"message\":\"Incorrect API key\"}}");

        ConnectivityLevelResult result = checker.checkL1(target("bad-key")).block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.AUTH_FAILED, result.errorType());
        assertEquals(401, result.upstreamStatus());
    }

    @Test
    void l1ClassifiesTimeoutWithinBudget() {
        properties.setCheckL1TimeoutSeconds(1);
        server.enqueueDelayed(MODELS, 3000);

        ConnectivityLevelResult result = checker.checkL1(target("k1")).block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.TIMEOUT, result.errorType());
    }

    @Test
    void l2SendsMinimalNonStreamingProbe() {
        server.enqueue(
                CHAT,
                200,
                "{\"id\":\"x\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"probe-model\","
                        + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\".\"},"
                        + "\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}");

        ConnectivityLevelResult result =
                checker.checkL2(target("k1"), "probe-model").block();

        assertEquals(ConnectivityStatusEnum.PASS, result.status());
        assertEquals("probe-model", result.model());
        String body = server.lastBody(CHAT);
        assertTrue(body.contains("\"model\":\"probe-model\""));
        assertTrue(body.contains("\"max_tokens\":1"));
        assertTrue(body.contains("ping"));
        assertFalse(body.contains("\"stream\":true"));
    }

    @Test
    void l2ClassifiesRateLimited() {
        server.enqueue(CHAT, 429, "{\"error\":{\"message\":\"Rate limit reached\"}}");

        ConnectivityLevelResult result =
                checker.checkL2(target("k1"), "probe-model").block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.RATE_LIMITED, result.errorType());
        assertEquals(429, result.upstreamStatus());
    }

    @Test
    void l2ClassifiesMalformedBodyAsBadResponse() {
        server.enqueue(CHAT, 200, "<html>not json</html>");

        ConnectivityLevelResult result =
                checker.checkL2(target("k1"), "probe-model").block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.BAD_RESPONSE, result.errorType());
    }

    @Test
    void emptyKeyAuthFailureIsReportedAsNoKeyConfigured() {
        server.enqueue(MODELS, 401, "{\"error\":{\"message\":\"Incorrect API key\"}}");

        ConnectivityLevelResult result = checker.checkL1(target("")).block();

        assertEquals(ConnectivityStatusEnum.FAIL, result.status());
        assertEquals(ConnectivityErrorTypeEnum.NO_KEY_CONFIGURED, result.errorType());
    }
}
