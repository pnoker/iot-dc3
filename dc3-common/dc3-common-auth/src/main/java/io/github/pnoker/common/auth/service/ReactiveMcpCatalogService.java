package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

/** Reactive MCP administration query boundary. */
public interface ReactiveMcpCatalogService {
    Mono<OffsetPage<McpToolVO>> listTools(String keyword, String riskLevel, PageRequest page);

    Mono<OffsetPage<McpAuditVO>> listAudit(Long tenantId, Long principalId, String toolId, String status,
                                            String riskLevel, PageRequest page);
}
