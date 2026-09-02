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
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcMcpCatalogStore implements ReactiveMcpCatalogStore {
    private static final String TOOL = "dc3_auth.dc3_mcp_tool_catalog";
    private static final String AUDIT = "dc3_auth.dc3_mcp_audit_log";
    private final DatabaseClient client;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<OffsetPage<McpToolRecord>> listTools(String keyword, String riskLevel, PageRequest page) {
        StringBuilder predicate = new StringBuilder(" WHERE deleted=0");
        if (keyword != null && !keyword.isBlank())
            predicate.append(
                    " AND (tool_id LIKE :keyword OR tool_name LIKE :keyword OR tool_title LIKE :keyword OR remark LIKE :keyword)");
        if (riskLevel != null && !riskLevel.isBlank()) predicate.append(" AND risk_level=:risk_level");
        DatabaseClient.GenericExecuteSpec count = client.sql("SELECT COUNT(*) AS total FROM " + TOOL + predicate);
        DatabaseClient.GenericExecuteSpec rows = client.sql(
                        "SELECT id,tool_id,tool_name,tool_title,tool_category,service_name,api_code,permission_code,http_method,api_path,schema_hash,risk_level,read_only_hint,destructive_hint,idempotent_hint,open_world_hint,enable_flag,remark,tool_ext FROM "
                                + TOOL + predicate + orderBy(page, false) + " LIMIT :limit OFFSET :offset")
                .bind("limit", page.limit())
                .bind("offset", page.offset());
        if (keyword != null && !keyword.isBlank()) {
            String value = "%" + keyword.trim() + "%";
            count = count.bind("keyword", value);
            rows = rows.bind("keyword", value);
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            count = count.bind("risk_level", riskLevel.trim());
            rows = rows.bind("risk_level", riskLevel.trim());
        }
        Mono<Long> total = count.map(
                        (row, metadata) -> row.get("total", Number.class).longValue())
                .one();
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map(this::tool)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Mono<OffsetPage<McpAuditCommand>> listAudit(
            Long tenantId, Long principalId, String toolId, String status, String riskLevel, PageRequest page) {
        StringBuilder predicate = new StringBuilder(" WHERE tenant_id=:tenant_id AND deleted=0");
        if (principalId != null) predicate.append(" AND principal_id=:principal_id");
        if (toolId != null && !toolId.isBlank()) predicate.append(" AND tool_id=:tool_id");
        if (status != null && !status.isBlank()) predicate.append(" AND status=:status");
        if (riskLevel != null && !riskLevel.isBlank()) predicate.append(" AND risk_level=:risk_level");
        DatabaseClient.GenericExecuteSpec count = client.sql("SELECT COUNT(*) AS total FROM " + AUDIT + predicate);
        DatabaseClient.GenericExecuteSpec rows = client.sql(
                        "SELECT id,trace_id,tenant_id,principal_id,principal_type,client_id,connection_id,tool_id,tool_name,permission_code,risk_level,confirm_id,idempotency_key,argument_digest,status,error_code,duration_ms,client_name,client_version,remote_ip,create_time FROM "
                                + AUDIT + predicate + orderBy(page, true) + " LIMIT :limit OFFSET :offset")
                .bind("limit", page.limit())
                .bind("offset", page.offset());
        count = count.bind("tenant_id", tenantId);
        rows = rows.bind("tenant_id", tenantId);
        if (principalId != null) {
            count = count.bind("principal_id", principalId);
            rows = rows.bind("principal_id", principalId);
        }
        if (toolId != null && !toolId.isBlank()) {
            count = count.bind("tool_id", toolId.trim());
            rows = rows.bind("tool_id", toolId.trim());
        }
        if (status != null && !status.isBlank()) {
            count = count.bind("status", status.trim());
            rows = rows.bind("status", status.trim());
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            count = count.bind("risk_level", riskLevel.trim());
            rows = rows.bind("risk_level", riskLevel.trim());
        }
        Mono<Long> total = count.map(
                        (row, metadata) -> row.get("total", Number.class).longValue())
                .one();
        DatabaseClient.GenericExecuteSpec itemRows = rows;
        return total.flatMap(totalCount -> itemRows.map(this::audit)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, page.offset(), page.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private McpToolRecord tool(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        McpToolRecord value = new McpToolRecord();
        value.setId(row.get("id", Long.class));
        value.setToolId(row.get("tool_id", String.class));
        value.setToolName(row.get("tool_name", String.class));
        value.setToolTitle(row.get("tool_title", String.class));
        value.setToolCategory(row.get("tool_category", String.class));
        value.setServiceName(row.get("service_name", String.class));
        value.setApiCode(row.get("api_code", String.class));
        value.setPermissionCode(row.get("permission_code", String.class));
        value.setHttpMethod(row.get("http_method", String.class));
        value.setApiPath(row.get("api_path", String.class));
        value.setSchemaHash(row.get("schema_hash", String.class));
        value.setRiskLevel(row.get("risk_level", String.class));
        value.setReadOnlyHint(number(row.get("read_only_hint")));
        value.setDestructiveHint(number(row.get("destructive_hint")));
        value.setIdempotentHint(number(row.get("idempotent_hint")));
        value.setOpenWorldHint(number(row.get("open_world_hint")));
        value.setEnableFlag(number(row.get("enable_flag")));
        value.setRemark(row.get("remark", String.class));
        value.setToolExt(row.get("tool_ext", String.class));
        return value;
    }

    private McpAuditCommand audit(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        McpAuditCommand value = new McpAuditCommand();
        value.setId(row.get("id", Long.class));
        value.setTraceId(row.get("trace_id", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setClientId(row.get("client_id", String.class));
        value.setConnectionId(row.get("connection_id", Long.class));
        value.setToolId(row.get("tool_id", String.class));
        value.setToolName(row.get("tool_name", String.class));
        value.setPermissionCode(row.get("permission_code", String.class));
        value.setRiskLevel(row.get("risk_level", String.class));
        value.setConfirmId(row.get("confirm_id", String.class));
        value.setIdempotencyKey(row.get("idempotency_key", String.class));
        value.setArgumentDigest(row.get("argument_digest", String.class));
        value.setStatus(row.get("status", String.class));
        value.setErrorCode(row.get("error_code", String.class));
        Number duration = row.get("duration_ms", Number.class);
        value.setDurationMs(duration == null ? null : duration.longValue());
        value.setClientVersion(row.get("client_version", String.class));
        value.setRemoteIp(row.get("remote_ip", String.class));
        value.setCreateTime(time(row.get("create_time")));
        return value;
    }

    private Byte number(Object raw) {
        return raw instanceof Number value ? value.byteValue() : null;
    }

    private String orderBy(PageRequest page, boolean audit) {
        if (page.sort().isEmpty())
            return audit ? " ORDER BY create_time DESC,id DESC" : " ORDER BY tool_category,tool_name,tool_id";
        List<String> clauses = new ArrayList<>();
        for (var spec : page.sort()) {
            String column = audit
                    ? switch (spec.field()) {
                        case "id" -> "id";
                        case "createTime" -> "create_time";
                        case "tenantId" -> "tenant_id";
                        case "principalId" -> "principal_id";
                        case "toolId" -> "tool_id";
                        case "status" -> "status";
                        default ->
                            throw new IllegalArgumentException("unsupported MCP audit sort field: " + spec.field());
                    }
                    : switch (spec.field()) {
                        case "id" -> "id";
                        case "toolId" -> "tool_id";
                        case "toolName" -> "tool_name";
                        case "toolCategory" -> "tool_category";
                        case "serviceName" -> "service_name";
                        case "apiCode" -> "api_code";
                        case "riskLevel" -> "risk_level";
                        case "createTime" -> "create_time";
                        default ->
                            throw new IllegalArgumentException("unsupported MCP tool sort field: " + spec.field());
                    };
            clauses.add(column + " " + spec.direction().name());
        }
        String id = "id ";
        if (clauses.stream().noneMatch(value -> value.startsWith(id))) clauses.add("id ASC");
        return " ORDER BY " + String.join(",", clauses);
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof java.time.OffsetDateTime value)
            return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }
}
