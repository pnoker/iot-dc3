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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC persistence port for the MCP authorization hot path. */
public interface ReactiveMcpRuntimeStore {
    /** Resolve the mcp runtime by its access token jti. */
    Mono<OAuthAuthorizationRecord> getAuthorizationByAccessTokenJti(String jti);

    /** Load the connection for the request. */
    Mono<McpConnectionRecord> getConnection(Long id);

    /** Resolve the tool record for the principal's connection and tool name. */
    Mono<McpToolRecord> resolveTool(
            Long tenantId, Long principalId, Long connectionId, String toolName, boolean allowHighRisk);

    /** Stream tools matching the request. */
    Flux<McpToolRecord> listTools(Long tenantId, Long principalId, Long connectionId, boolean allowHighRisk);

    /** Stamp the connection's last-used time. */
    Mono<Boolean> touchConnection(Long id, LocalDateTime usedAt);

    /** Load the idempotent consumption record by its key. */
    Mono<McpToolConfirmationRecord> getConsumedByIdempotency(Long connectionId, String key);

    /** Resolve the idempotent consumption record by its key. */
    Mono<McpToolConfirmationRecord> getByIdempotency(Long connectionId, String key);

    /** Load the confirmation for the request. */
    Mono<McpToolConfirmationRecord> getConfirmation(String confirmId);

    /** Insert one confirmation and emit the stored row. */
    Mono<Integer> insertConfirmation(McpToolConfirmationRecord confirmation);

    /** Consume the pending confirmation, returning the rows updated. */
    Mono<Integer> consumeConfirmation(Long id, LocalDateTime consumedAt);
    /** Atomically claims a pending confirmation after re-checking its full security binding. */
    default Mono<Integer> consumeConfirmation(
            String confirmId,
            Long tenantId,
            Long principalId,
            Long connectionId,
            String toolId,
            String argumentDigest,
            LocalDateTime consumedAt) {
        return Mono.error(new UnsupportedOperationException("atomic confirmation claim is not implemented"));
    }

    /** Insert one audit and emit the stored row. */
    Mono<Integer> insertAudit(McpAuditCommand command);
}
