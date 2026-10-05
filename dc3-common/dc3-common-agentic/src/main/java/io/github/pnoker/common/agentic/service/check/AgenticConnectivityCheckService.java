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

import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckResult;
import io.github.pnoker.common.agentic.entity.vo.ProviderCheckRequestVO;
import io.github.pnoker.common.entity.common.RequestHeader;
import reactor.core.publisher.Mono;

/**
 * Provider/model connectivity check orchestration: resolves the effective
 * target (saved provider by id, or an unsaved draft), runs the L1/L2 probes
 * through the protocol-specific {@link AgenticConnectivityChecker}, merges
 * concurrent checks of the same target, caches results for a short TTL, and
 * persists the latest outcome as the {@code last_check_*} health profile.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface AgenticConnectivityCheckService {

    /**
     * Check a provider: saved provider by {@code request.id} (missing fields
     * fall back to stored values) or an unsaved draft configuration. Draft
     * checks never persist a health profile.
     *
     * @param request check request
     * @param header  authenticated principal header
     * @return aggregate L1/L2 outcome; a failed probe is a diagnosis here,
     *         not an error signal
     */
    Mono<ConnectivityCheckResult> checkProvider(ProviderCheckRequestVO request, RequestHeader.PrincipalHeader header);

    /**
     * Check one model configuration end to end (L2 only) using its stored
     * model identifier and provider credentials; persists the outcome on the
     * config row.
     *
     * @param id     model config id
     * @param header authenticated principal header
     * @return L2 outcome for the config's model
     */
    Mono<ConnectivityCheckResult> checkModelConfig(Long id, RequestHeader.PrincipalHeader header);
}
