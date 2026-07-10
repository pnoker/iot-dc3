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

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.entity.common.RequestHeader;

import java.util.List;


/**
 * Service for managing LLM model configuration options.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface ModelConfigService {

    /**
     * List enabled model options for the picker, including a fallback default entry.
     *
     * @return the enabled model options
     */
    List<ModelVO> listOptions();

    /**
     * List all model configurations with their provider names.
     *
     * @return all model configurations
     */
    List<ModelConfigBO> listConfigs();

    /**
     * Add a model configuration, maintaining the single-default invariant.
     *
     * @param entityBO the model configuration to add
     * @param header   authenticated caller principal and tenant
     * @return the added model configuration
     */
    ModelConfigBO add(ModelConfigBO entityBO, RequestHeader.PrincipalHeader header);

    /**
     * Update a model configuration.
     *
     * @param entityBO the model configuration to update
     * @param header   authenticated caller principal and tenant
     * @return the updated model configuration
     */
    ModelConfigBO update(ModelConfigBO entityBO, RequestHeader.PrincipalHeader header);

    /**
     * Delete a model configuration by id.
     *
     * @param id the model configuration id
     */
    void delete(Long id);

}
