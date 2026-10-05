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
package io.github.pnoker.common.agentic.service.check;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import reactor.core.publisher.Mono;

/**
 * Protocol-specific connectivity probe. Implementations build a one-shot SDK
 * client per probe (never the cached {@code ChatClientFactory} clients, never
 * the spring.ai fallback), with the probe timeout budget and zero retries.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface AgenticConnectivityChecker {

    /**
     * Whether this checker handles the provider type.
     *
     * @param providerType provider protocol type
     * @return true when this implementation probes the given protocol
     */
    boolean supports(AgenticModelProviderTypeEnum providerType);

    /**
     * L1 probe: list the models the endpoint exposes. Zero token cost;
     * validates DNS/TLS/routing/credentials and returns the visible model ids.
     *
     * @param target effective provider target (type, base URL, API key)
     * @return probe outcome with the model ids on success
     */
    Mono<ConnectivityLevelResult> checkL1(ModelProviderBO target);

    /**
     * L2 probe: one minimal non-streaming chat completion. Verifies the model
     * name, quota/billing and the full inference chain; judged by HTTP status
     * only, never by content quality.
     *
     * @param target effective provider target (type, base URL, API key)
     * @param model  model identifier to probe with
     * @return probe outcome naming the model it used
     */
    Mono<ConnectivityLevelResult> checkL2(ModelProviderBO target, String model);
}
