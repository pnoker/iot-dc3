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
package io.github.pnoker.common.agentic.service.direct;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.request.DirectQueryRequest;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Queue;

/**
 * Routes deterministic backend lookups that can be resolved without involving the model.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AgenticDirectBackendService {

    private final DataMonitorDirectBackendProvider dataMonitorProvider;

    public AgenticDirectBackendService(DataMonitorDirectBackendProvider dataMonitorProvider) {
        this.dataMonitorProvider = dataMonitorProvider;
    }

    public DirectBackendResult build(DirectQueryRequest directQuery, RequestHeader.UserHeader userHeader,
                                     Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        try {
            if (Objects.nonNull(directQuery)) {
                if (directQuery.isPointValueQuery()) {
                    return dataMonitorProvider.build(directQuery, userHeader, toolEvents);
                }
                return DirectBackendResult.direct(DirectAnswer.message("查询失败",
                        "不支持的确定性查询类型：" + directQuery.getType()));
            }
        } catch (Exception e) {
            log.warn("Agentic direct backend lookup failed, tenantId={}, userId={}", userHeader.getTenantId(),
                    userHeader.getUserId(), e);
            offerToolEvent(toolEvents, "directContext", "agentic",
                    "Backend context query failed: " + e.getMessage());
            if (dataMonitorProvider.isResolvedPointValueRequest(directQuery)) {
                return dataMonitorProvider.failedQueryResult();
            }
        }
        return null;
    }

    private void offerToolEvent(Queue<AgenticRequestContext.ToolEvent> toolEvents, String toolName, String domain,
                                String description) {
        if (Objects.nonNull(toolEvents)) {
            toolEvents.offer(new AgenticRequestContext.ToolEvent(toolName, domain, description,
                    Instant.now().toEpochMilli()));
        }
    }

}
