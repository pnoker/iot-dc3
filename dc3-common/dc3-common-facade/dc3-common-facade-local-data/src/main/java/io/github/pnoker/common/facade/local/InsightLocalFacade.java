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
package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.insight.InsightDispatcher;
import io.github.pnoker.common.facade.api.InsightFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * In-process InsightFacade backed by the shared {@link InsightDispatcher}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightLocalFacade implements InsightFacade {

    private final InsightDispatcher insightDispatcher;

    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> invokeReactive(Long tenantId, String operation, String requestJson) {
        return insightDispatcher.invoke(tenantId, operation, requestJson).map(objectMapper::writeValueAsString);
    }
}
