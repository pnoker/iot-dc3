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
package io.github.pnoker.common.agentic.entity.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import org.junit.jupiter.api.Test;

/**
 * Regression cover for the provider credential mapping: the API key MUST
 * flow request→BO (a @Mapping(ignore) on this direction silently dropped
 * every saved credential — every model provider ended up keyless and every
 * chat failed with "requires apiKey or workloadIdentity"), while the
 * response direction must never expose it.
 */
class ModelProviderBuilderApiKeyTest {

    private final ModelProviderBuilderImpl builder = new ModelProviderBuilderImpl();

    @Test
    void apiKeySurvivesTheSaveDirection() {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setName("probe");
        vo.setBaseUrl("https://example.invalid/v1");
        vo.setApiKey("secret-123");

        ModelProviderBO bo = builder.buildBOByVO(vo);

        assertThat(bo.getApiKey()).isEqualTo("secret-123");
        assertThat(bo.getBaseUrl()).isEqualTo("https://example.invalid/v1");
    }

    @Test
    void apiKeyNeverLeaksThroughTheResponseDirection() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("probe");
        bo.setApiKey("secret-123");

        ModelProviderVO vo = builder.buildVOByBO(bo);

        assertThat(vo.getApiKey()).isNull();
    }
}
