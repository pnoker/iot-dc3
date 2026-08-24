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

import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantLineHandlerImplTest {

    private final TenantLineHandlerImpl handler = new TenantLineHandlerImpl();

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void getTenantIdReturnsLongValueWhenBound() {
        TenantContextHolder.setTenantId(42L);
        assertThat(handler.getTenantId()).isInstanceOf(LongValue.class);
        assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(42L);
    }

    @Test
    void getTenantIdThrowsWhenNullAndNotIgnored() {
        // no setTenantId, not ignored → fail-closed
        assertThatThrownBy(() -> handler.getTenantId())
                .isInstanceOf(TenantNotScopedException.class);
    }

    @Test
    void ignoreTableTrueForWhitelistedTables() {
        // All 11 tenant_id-less tables whitelisted in IGNORE_TABLES must be skipped.
        assertThat(handler.ignoreTable("dc3_tenant")).isTrue();
        assertThat(handler.ignoreTable("dc3_principal")).isTrue();
        assertThat(handler.ignoreTable("dc3_user")).isTrue();
        assertThat(handler.ignoreTable("dc3_local_credential")).isTrue();
        assertThat(handler.ignoreTable("dc3_external_identity")).isTrue();
        assertThat(handler.ignoreTable("dc3_resource")).isTrue();
        assertThat(handler.ignoreTable("dc3_role_resource_bind")).isTrue();
        assertThat(handler.ignoreTable("dc3_api")).isTrue();
        assertThat(handler.ignoreTable("dc3_mcp_tool_catalog")).isTrue();
        assertThat(handler.ignoreTable("dc3_mcp_connection_tool")).isTrue();
        assertThat(handler.ignoreTable("dc3_menu")).isTrue();
    }

    @Test
    void ignoreTableFalseForTenantScopedTable() {
        TenantContextHolder.setTenantId(1L); // not ignored
        assertThat(handler.ignoreTable("dc3_device")).isFalse();
    }

    @Test
    void ignoreTableTrueForAllTablesWhenIgnored() {
        TenantContextHolder.runIgnoreAction(() -> {
            assertThat(handler.ignoreTable("dc3_device")).isTrue();  // normally scoped
            assertThat(handler.ignoreTable("dc3_point")).isTrue();
        });
    }

    @Test
    void getTenantIdColumnIsTenantId() {
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }
}
