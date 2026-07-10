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

package io.github.pnoker.common.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * MyBatis-Plus tenant-line handler backed by {@link TenantContextHolder}.
 * <p>
 * Fail-closed: when no tenant id is bound and the thread is not in a
 * {@link TenantContextHolder#runIgnore} / {@link TenantContextHolder#runIgnoreAction}
 * scope, {@link #getTenantId()} throws {@link TenantNotScopedException} rather than
 * letting the query run unscoped.
 * Tables without a tenant_id column (system/lookup tables) are whitelisted in
 * {@link #ignoreTable} so the interceptor does not inject a non-existent column.
 */
@Component
public class TenantLineHandlerImpl implements TenantLineHandler {

    /**
     * Tables without a tenant_id column — interceptor must NOT inject for these.
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "dc3_tenant",
            "dc3_principal",
            "dc3_user",
            "dc3_local_credential",
            "dc3_external_identity",
            "dc3_resource",
            "dc3_role_resource_bind",
            "dc3_api",
            "dc3_mcp_tool_catalog",
            "dc3_mcp_connection_tool",
            "dc3_menu"
    );

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // isIgnored() paths are routed through ignoreTable (returns true) and never reach here.
            throw new TenantNotScopedException(
                    "Tenant-scoped query executed without tenant id on thread; "
                            + "wrap cross-tenant/tenant-free work in TenantContextHolder.runIgnore/runIgnoreAction");
        }
        return new LongValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // Ignored context (runIgnore/runIgnoreAction) → skip injection for ALL tables.
        // Otherwise skip whitelisted (tenant_id-less) tables.
        return TenantContextHolder.isIgnored() || IGNORE_TABLES.contains(tableName);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
}
