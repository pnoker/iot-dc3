package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

/** Reactive query port for MCP administration projections. */
public interface ReactiveMcpCatalogStore {
    Mono<OffsetPage<McpToolRecord>> listTools(String keyword, String riskLevel, PageRequest page);

    Mono<OffsetPage<McpAuditCommand>> listAudit(Long tenantId, Long principalId, String toolId, String status,
                                                 String riskLevel, PageRequest page);
}
