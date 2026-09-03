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
package io.github.pnoker.db.postgres.auth;

import io.github.pnoker.common.auth.repository.ReactiveTenantDictionaryStore;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Explicit SQL adapter for the enabled-tenant dictionary projection. */
@Repository
@RequiredArgsConstructor
@ConditionalOnClass(DatabaseClient.class)
public class R2dbcTenantDictionaryStore implements ReactiveTenantDictionaryStore {
    private static final String TABLE = "dc3_auth.dc3_tenant";
    private final DatabaseClient databaseClient;

    @Override
    public Flux<TenantDO> listEnabled() {
        return databaseClient
                .sql("SELECT id,tenant_name FROM " + TABLE
                        + " WHERE enable_flag=0 AND deleted=0 ORDER BY tenant_name,id")
                .map((row, metadata) -> {
                    TenantDO tenant = new TenantDO();
                    tenant.setId(row.get("id", Long.class));
                    tenant.setTenantName(row.get("tenant_name", String.class));
                    return tenant;
                })
                .all();
    }
}
