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
package io.github.pnoker.common.agentic.service;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.entity.common.RequestHeader;

import java.util.List;


/**
 * Service for managing LLM model provider registrations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface ModelProviderService {

    /**
     * List all registered model providers.
     *
     * @return all model providers
     */
    List<ModelProviderBO> list();

    /**
     * Register a model provider.
     *
     * @param entityBO the provider to add
     * @param header   authenticated caller principal and tenant
     * @return the added provider
     */
    ModelProviderBO add(ModelProviderBO entityBO, RequestHeader.PrincipalHeader header);

    /**
     * Update a model provider, evicting any cached ChatClient built from it.
     *
     * @param entityBO the provider to update
     * @param header   authenticated caller principal and tenant
     * @return the updated provider
     */
    ModelProviderBO update(ModelProviderBO entityBO, RequestHeader.PrincipalHeader header);

    /**
     * Delete a model provider and clean up its cached ChatClient.
     *
     * @param id the provider id
     */
    void delete(Long id);

}
