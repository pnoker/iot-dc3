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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** SQL adapter for the command dispatch metadata projection. */
@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcPointCommandContext implements ReactivePointCommandContext {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<FacadeDeviceBO> device(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.empty();
        return databaseClient
                .sql("SELECT id, device_name, device_code, driver_id, profile_id, enable_flag, tenant_id, "
                        + "signature, version FROM dc3_manager.dc3_device WHERE tenant_id = :tenant_id AND id = :id "
                        + "AND deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", deviceId)
                .map((row, metadata) -> {
                    FacadeDeviceBO value = new FacadeDeviceBO();
                    value.setId(row.get("id", Long.class));
                    value.setDeviceName(row.get("device_name", String.class));
                    value.setDeviceCode(row.get("device_code", String.class));
                    value.setDriverId(row.get("driver_id", Long.class));
                    value.setProfileId(row.get("profile_id", Long.class));
                    value.setTenantId(row.get("tenant_id", Long.class));
                    value.setSignature(row.get("signature", String.class));
                    value.setVersion(row.get("version", Integer.class));
                    value.setEnableFlag(EnableFlagEnum.ofIndex(index(row.get("enable_flag", Number.class))));
                    return value;
                })
                .one();
    }

    @Override
    public Mono<FacadePointBO> point(Long tenantId, Long pointId) {
        if (tenantId == null || pointId == null) return Mono.empty();
        return databaseClient
                .sql("SELECT id, point_name, point_code, point_type_flag, rw_flag, profile_id, enable_flag, "
                        + "tenant_id, signature, version FROM dc3_manager.dc3_point WHERE tenant_id = :tenant_id AND id = :id "
                        + "AND deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", pointId)
                .map((row, metadata) -> {
                    FacadePointBO value = new FacadePointBO();
                    value.setId(row.get("id", Long.class));
                    value.setPointName(row.get("point_name", String.class));
                    value.setPointCode(row.get("point_code", String.class));
                    value.setProfileId(row.get("profile_id", Long.class));
                    value.setTenantId(row.get("tenant_id", Long.class));
                    value.setSignature(row.get("signature", String.class));
                    value.setVersion(row.get("version", Integer.class));
                    value.setPointTypeFlag(PointTypeEnum.ofIndex(index(row.get("point_type_flag", Number.class))));
                    value.setRwFlag(RwTypeEnum.ofIndex(index(row.get("rw_flag", Number.class))));
                    value.setEnableFlag(EnableFlagEnum.ofIndex(index(row.get("enable_flag", Number.class))));
                    return value;
                })
                .one();
    }

    @Override
    public Mono<FacadeDriverBO> driverByDevice(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.empty();
        return databaseClient
                .sql(
                        "SELECT d.id, d.driver_name, d.driver_code, d.service_name, d.service_host, "
                                + "d.driver_type_flag, d.enable_flag, d.tenant_id, d.signature, d.version FROM dc3_manager.dc3_driver d "
                                + "JOIN dc3_manager.dc3_device v ON v.driver_id = d.id AND v.tenant_id = d.tenant_id "
                                + "WHERE d.tenant_id = :tenant_id AND v.id = :device_id AND d.deleted = 0 AND v.deleted = 0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map((row, metadata) -> {
                    FacadeDriverBO value = new FacadeDriverBO();
                    value.setId(row.get("id", Long.class));
                    value.setDriverName(row.get("driver_name", String.class));
                    value.setDriverCode(row.get("driver_code", String.class));
                    value.setServiceName(row.get("service_name", String.class));
                    value.setServiceHost(row.get("service_host", String.class));
                    value.setTenantId(row.get("tenant_id", Long.class));
                    value.setSignature(row.get("signature", String.class));
                    value.setVersion(row.get("version", Integer.class));
                    value.setDriverTypeFlag(DriverTypeEnum.ofIndex(index(row.get("driver_type_flag", Number.class))));
                    value.setEnableFlag(EnableFlagEnum.ofIndex(index(row.get("enable_flag", Number.class))));
                    return value;
                })
                .one();
    }

    @Override
    public Mono<FacadeDeviceOwnerBO> activeOwner(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.empty();
        return databaseClient
                .sql("SELECT l.driver_id, l.owner_node, l.fencing_token FROM dc3_manager.dc3_device_lease l "
                        + "JOIN dc3_manager.dc3_driver_instance i ON i.tenant_id = l.tenant_id AND i.driver_id = l.driver_id "
                        + "AND i.node_id = l.owner_node WHERE l.tenant_id = :tenant_id AND l.device_id = :device_id "
                        + "AND i.lease_until > CURRENT_TIMESTAMP LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map((row, metadata) -> new FacadeDeviceOwnerBO(
                        row.get("driver_id", Long.class),
                        row.get("owner_node", String.class),
                        row.get("fencing_token", Long.class)))
                .one();
    }

    private Byte index(Number value) {
        return value == null ? null : value.byteValue();
    }
}
