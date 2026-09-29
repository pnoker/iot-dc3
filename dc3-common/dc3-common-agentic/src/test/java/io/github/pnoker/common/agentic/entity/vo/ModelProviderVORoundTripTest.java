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
package io.github.pnoker.common.agentic.entity.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Regression cover for the provider credential path: the API key must
 * survive request deserialization (WRITE_ONLY keeps it out of responses,
 * but the web stack's Jackson 3 mapper must still read it from requests —
 * a silent drop here left every model provider keyless and every chat
 * failing with "requires apiKey").
 */
class ModelProviderVORoundTripTest {

    // Mirror JsonUtil.getJsonMapper(): the production decoder config —
    // findAndAddModules + DEFAULT_VIEW_INCLUSION=false.
    private final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(tools.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .build();

    @Test
    void deserializesApiKeyFromRequestJson() {
        String json = """
                {"name":"probe","baseUrl":"https://example.invalid/v1","apiKey":"secret-123"}
                """;
        ModelProviderVO vo = mapper.readValue(json, ModelProviderVO.class);
        assertThat(vo.getApiKey()).isEqualTo("secret-123");
    }

    @Test
    void neverSerializesApiKeyIntoResponses() {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setApiKey("secret-123");
        String json = mapper.writeValueAsString(vo);
        assertThat(json).doesNotContain("secret-123");
    }
}
