package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;

/** R2DBC persistence port for the MCP authorization hot path. */
public interface ReactiveMcpRuntimeStore {
    Mono<OAuthAuthorizationRecord> getAuthorizationByAccessTokenJti(String jti);
    Mono<McpConnectionRecord> getConnection(Long id);
    Mono<McpToolRecord> resolveTool(Long tenantId, Long principalId, Long connectionId, String toolName, boolean allowHighRisk);
    Flux<McpToolRecord> listTools(Long tenantId, Long principalId, Long connectionId, boolean allowHighRisk);
    Mono<Boolean> touchConnection(Long id, LocalDateTime usedAt);
    Mono<McpToolConfirmationRecord> getConsumedByIdempotency(Long connectionId, String key);
    Mono<McpToolConfirmationRecord> getByIdempotency(Long connectionId, String key);
    Mono<McpToolConfirmationRecord> getConfirmation(String confirmId);
    Mono<Integer> insertConfirmation(McpToolConfirmationRecord confirmation);
    Mono<Integer> consumeConfirmation(Long id, LocalDateTime consumedAt);
    /** Atomically claims a pending confirmation after re-checking its full security binding. */
    default Mono<Integer> consumeConfirmation(String confirmId, Long tenantId, Long principalId, Long connectionId,
                                              String toolId, String argumentDigest, LocalDateTime consumedAt) {
        return Mono.error(new UnsupportedOperationException("atomic confirmation claim is not implemented"));
    }
    Mono<Integer> insertAudit(McpAuditCommand command);
}
