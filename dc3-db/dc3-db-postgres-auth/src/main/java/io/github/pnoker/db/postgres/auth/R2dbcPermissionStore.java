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

import io.github.pnoker.common.auth.repository.ReactivePermissionStore;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** R2DBC read adapter for the effective principal permission projection. */
@Repository
@RequiredArgsConstructor
@ConditionalOnClass(DatabaseClient.class)
public class R2dbcPermissionStore implements ReactivePermissionStore {
    private final DatabaseClient databaseClient;

    @Override
    public Flux<String> listResourceCodes(Long tenantId, Long principalId) {
        if (tenantId == null || tenantId <= 0 || principalId == null || principalId <= 0) return Flux.empty();
        String sql = "SELECT DISTINCT r.resource_code FROM dc3_auth.dc3_role_principal_bind rp "
                + "JOIN dc3_auth.dc3_role role ON role.id=rp.role_id AND role.tenant_id=rp.tenant_id "
                + "JOIN dc3_auth.dc3_role_resource_bind rr ON rr.role_id=role.id AND rr.deleted=0 "
                + "JOIN dc3_auth.dc3_resource r ON r.id=rr.resource_id AND r.deleted=0 "
                + "WHERE rp.tenant_id=:tenant_id AND rp.principal_id=:principal_id "
                + "AND rp.deleted=0 AND role.deleted=0 AND role.enable_flag=0 AND r.enable_flag=0 "
                + "AND r.resource_code IS NOT NULL AND r.resource_code<>'' ORDER BY r.resource_code";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .map((row, metadata) -> row.get("resource_code", String.class))
                .all();
    }
}
