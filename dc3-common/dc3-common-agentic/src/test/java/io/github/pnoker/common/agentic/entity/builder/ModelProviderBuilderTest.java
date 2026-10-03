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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Contract test: the provider API key is write-only. It must flow from request payloads into the
 * business object, and must never cross back into a value object or its JSON serialization.
 */
class ModelProviderBuilderTest {

    private final ModelProviderBuilder builder = new ModelProviderBuilderImpl();

    private ModelProviderBO boWithApiKey() {
        ModelProviderBO entityBO = new ModelProviderBO();
        entityBO.setName("provider");
        entityBO.setBaseUrl("https://api.example.com/v1");
        entityBO.setApiKey("sk-store-secret");
        return entityBO;
    }

    @Test
    void omitsApiKeyWhenBuildingVoFromBo() {
        ModelProviderVO entityVO = builder.buildVOByBO(boWithApiKey());

        assertNull(entityVO.getApiKey());
    }

    @Test
    void omitsApiKeyWhenBuildingVoListFromBoList() {
        List<ModelProviderVO> entityVOList = builder.buildVOListByBOList(List.of(boWithApiKey()));

        assertNull(entityVOList.get(0).getApiKey());
    }

    @Test
    void carriesApiKeyFromRequestVoToBo() {
        ModelProviderVO entityVO = new ModelProviderVO();
        entityVO.setApiKey("sk-request-secret");

        ModelProviderBO entityBO = builder.buildBOByVO(entityVO);

        assertEquals("sk-request-secret", entityBO.getApiKey());
    }

    @Test
    void jacksonSerializationOmitsApiKeyEvenWhenPresent() throws Exception {
        ModelProviderVO entityVO = new ModelProviderVO();
        entityVO.setApiKey("sk-should-not-serialize");

        String json = new ObjectMapper().writeValueAsString(entityVO);

        assertFalse(json.contains("apiKey"));
        assertFalse(json.contains("sk-should-not-serialize"));
    }

    @Test
    void jacksonDeserializationAcceptsApiKey() throws Exception {
        ModelProviderVO entityVO = new ObjectMapper().readValue("{\"apiKey\":\"sk-inbound\"}", ModelProviderVO.class);

        assertEquals("sk-inbound", entityVO.getApiKey());
    }
}
