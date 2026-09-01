package io.github.pnoker.common.auth.repository;

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
        return databaseClient.sql("SELECT id,tenant_name FROM " + TABLE
                        + " WHERE enable_flag=0 AND deleted=0 ORDER BY tenant_name,id")
                .map((row, metadata) -> {
                    TenantDO tenant = new TenantDO();
                    tenant.setId(row.get("id", Long.class));
                    tenant.setTenantName(row.get("tenant_name", String.class));
                    return tenant;
                }).all();
    }
}
