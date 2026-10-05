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
package io.github.pnoker.common.agentic.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Transport-level 429 behaviour of the openai-java OkHttp client, observed
 * against a real MockWebServer. These tests pin the retry contract the
 * agentic transport relies on: the SDK retries rate limits with exponential
 * backoff by default (depth 2) and honours an explicit maxRetries setting —
 * the knob ChatClientFactory and OpenAiCompatibleAgenticRuntime now set from
 * AgenticProperties.transportMaxRetries.
 *
 * @author pnoker
 * @since 2016.10.1
 */
class TransportRetryBehaviorTest {

    private static final String COMPLETION_BODY = """
            {"id":"chatcmpl-mock","object":"chat.completion","created":1,"model":"mock-model",\
            "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],\
            "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}\
            """;

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private OpenAIClient client(Integer maxRetries) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                .baseUrl(server.url("/v1").toString())
                .apiKey("test-key");
        if (maxRetries != null) {
            builder.maxRetries(maxRetries);
        }
        return builder.build();
    }

    private static ChatCompletionCreateParams params() {
        return ChatCompletionCreateParams.builder()
                .model("mock-model")
                .addUserMessage("hi")
                .build();
    }

    private static MockResponse rateLimited() {
        // Retry-After: 0 keeps the exponential backoff from slowing the suite.
        return new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "0")
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"rate limited\",\"type\":\"rate_limit_error\"}}");
    }

    private static MockResponse completion() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(COMPLETION_BODY);
    }

    @Test
    void sdkRetriesRateLimitsByDefault() {
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(completion());

        String content = client(null)
                .chat()
                .completions()
                .create(params())
                .choices()
                .get(0)
                .message()
                .content()
                .orElse("");

        assertThat(content).isEqualTo("ok");
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void sdkGivesUpAfterDefaultRetries() {
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());

        assertThatThrownBy(() -> client(null).chat().completions().create(params()))
                .isInstanceOf(Exception.class);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void configuredRetryDepthIsHonoured() {
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(rateLimited());
        server.enqueue(completion());

        String content = client(4)
                .chat()
                .completions()
                .create(params())
                .choices()
                .get(0)
                .message()
                .content()
                .orElse("");

        assertThat(content).isEqualTo("ok");
        assertThat(server.getRequestCount()).isEqualTo(5);
    }
}
