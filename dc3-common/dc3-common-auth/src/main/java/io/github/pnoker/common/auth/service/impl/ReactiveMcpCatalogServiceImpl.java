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
package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.builder.McpAuditBuilder;
import io.github.pnoker.common.auth.entity.builder.McpToolBuilder;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.repository.ReactiveMcpCatalogStore;
import io.github.pnoker.common.auth.service.ReactiveMcpCatalogService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveMcpCatalogServiceImpl implements ReactiveMcpCatalogService {
    private final ReactiveMcpCatalogStore store;
    private final McpToolBuilder toolBuilder;
    private final McpAuditBuilder auditBuilder;

    @Override
    public Mono<OffsetPage<McpToolVO>> listTools(String keyword, String riskLevel, PageRequest page) {
        return store.listTools(keyword, riskLevel, page)
                .map(result -> OffsetPage.of(
                        result.items().stream()
                                .map(toolBuilder::buildVOByRecord)
                                .toList(),
                        result.offset(),
                        result.limit(),
                        result.total()));
    }

    @Override
    public Mono<OffsetPage<McpAuditVO>> listAudit(
            Long tenantId, Long principalId, String toolId, String status, String riskLevel, PageRequest page) {
        return store.listAudit(tenantId, principalId, toolId, status, riskLevel, page)
                .map(result -> OffsetPage.of(
                        result.items().stream()
                                .map(auditBuilder::buildVOByRecord)
                                .toList(),
                        result.offset(),
                        result.limit(),
                        result.total()));
    }
}
