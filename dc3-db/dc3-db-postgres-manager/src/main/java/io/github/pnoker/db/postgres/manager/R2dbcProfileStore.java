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
package io.github.pnoker.db.postgres.manager;

import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;

import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Explicit SQL adapter for manager profiles. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcProfileStore implements ReactiveProfileStore {

    private static final String TABLE = "dc3_manager.dc3_profile";
    private static final String COLUMNS = "p.id, p.profile_name, p.profile_code, p.profile_share_flag, "
            + "p.profile_type_flag, p.profile_ext, p.enable_flag, p.tenant_id, p.remark, p.signature, "
            + "p.version, p.creator_id, p.creator_name, p.create_time, p.operator_id, p.operator_name, p.operate_time";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final ObjectMapper objectMapper;
    private final R2dbcDialect dialect;

    @Override
    public Mono<Boolean> existsByName(Long tenantId, String profileName, Long excludingId) {
        if (tenantId == null || profileName == null || profileName.isBlank()) return Mono.just(false);
        String sql =
                "SELECT 1 FROM " + TABLE + " WHERE tenant_id=:tenant_id AND profile_name=:profile_name AND deleted=0"
                        + (excludingId == null ? "" : " AND id<>:excluding_id") + " LIMIT 1";
        DatabaseClient.GenericExecuteSpec query =
                databaseClient.sql(sql).bind("tenant_id", tenantId).bind("profile_name", profileName.trim());
        if (excludingId != null) query = query.bind("excluding_id", excludingId);
        return query.map((row, metadata) -> true).one().defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> hasAssociations(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.just(false);
        return databaseClient
                .sql(
                        "SELECT (EXISTS (SELECT 1 FROM dc3_manager.dc3_device WHERE tenant_id=:tenant_id AND profile_id=:id AND deleted=0) OR EXISTS (SELECT 1 FROM dc3_manager.dc3_point WHERE tenant_id=:tenant_id AND profile_id=:id AND deleted=0) OR EXISTS (SELECT 1 FROM dc3_manager.dc3_command WHERE tenant_id=:tenant_id AND profile_id=:id AND deleted=0) OR EXISTS (SELECT 1 FROM dc3_manager.dc3_event WHERE tenant_id=:tenant_id AND profile_id=:id AND deleted=0)) AS associated")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("associated", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<ProfileBO> insert(ProfileBO value) {
        if (value == null
                || value.getTenantId() == null
                || value.getProfileName() == null
                || value.getProfileName().isBlank())
            return Mono.error(new IllegalArgumentException("tenantId and profileName are required"));
        if (value.getId() == null) value.setId(UuidV7.nextLong());
        if (value.getVersion() == null) value.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "INSERT INTO " + TABLE
                                + " (id,profile_name,profile_code,profile_share_flag,profile_type_flag,profile_ext,enable_flag,tenant_id,remark,signature,version,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:profile_name,:profile_code,:profile_share_flag,:profile_type_flag,"
                                + dialect.jsonWriteExpression(":profile_ext")
                                + ",:enable_flag,:tenant_id,:remark,:signature,:version,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)")
                .bind("id", value.getId())
                .bind("profile_name", value.getProfileName().trim())
                .bind("profile_code", value.getProfileCode() == null ? "" : value.getProfileCode())
                .bind("profile_share_flag", index(value.getProfileShareFlag()))
                .bind("profile_type_flag", index(value.getProfileTypeFlag()))
                .bind("profile_ext", serialize(value.getProfileExt()))
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("version", value.getVersion())
                .bind("creator_id", value.getCreatorId() == null ? 0L : value.getCreatorId())
                .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName())
                .bind("create_time", value.getCreateTime() == null ? now : value.getCreateTime())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", value.getOperateTime() == null ? now : value.getOperateTime());
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? get(value.getTenantId(), value.getId())
                        : Mono.error(new IllegalStateException("profile insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<ProfileBO> update(ProfileBO value, int expectedVersion) {
        if (value == null || value.getTenantId() == null || value.getId() == null)
            return Mono.error(new IllegalArgumentException("tenantId and profile id are required"));
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET profile_name=:profile_name,profile_share_flag=:profile_share_flag,profile_type_flag=:profile_type_flag,profile_ext="
                                + dialect.jsonWriteExpression(":profile_ext")
                                + ",enable_flag=:enable_flag,remark=:remark,signature=:signature,version=version+1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE id=:id AND tenant_id=:tenant_id AND version=:expected_version AND deleted=0")
                .bind("profile_name", value.getProfileName().trim())
                .bind("profile_share_flag", index(value.getProfileShareFlag()))
                .bind("profile_type_flag", index(value.getProfileTypeFlag()))
                .bind("profile_ext", serialize(value.getProfileExt()))
                .bind("enable_flag", index(value.getEnableFlag()))
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("signature", value.getSignature() == null ? "" : value.getSignature())
                .bind("operator_id", value.getOperatorId() == null ? 0L : value.getOperatorId())
                .bind("operator_name", value.getOperatorName() == null ? "" : value.getOperatorName())
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .bind("id", value.getId())
                .bind("tenant_id", value.getTenantId())
                .bind("expected_version", expectedVersion);
        return transactionalOperator.transactional(query.fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(value.getTenantId(), value.getId()) : Mono.empty()));
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient
                .sql(
                        "UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND version=:expected_version AND deleted=0")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .bind("expected_version", expectedVersion)
                .bind("operator_id", operatorId == null ? 0L : operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .bind("operate_time", LocalDateTime.now(ZoneOffset.UTC))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Mono<ProfileBO> get(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " p"
                        + " WHERE p.tenant_id=:tenant_id AND p.id=:id AND p.deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<ProfileBO> getByNameAndType(Long tenantId, String name, ProfileTypeEnum type) {
        if (tenantId == null || name == null || name.isBlank() || type == null) {
            return Mono.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " p"
                        + " WHERE p.tenant_id=:tenant_id AND p.profile_name=:profile_name"
                        + " AND p.profile_type_flag=:profile_type AND p.deleted=0 LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("profile_name", name.trim())
                .bind("profile_type", type.getIndex())
                .map(this::map)
                .one();
    }

    @Override
    public Flux<ProfileBO> listByIds(Long tenantId, List<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        List<Long> distinctIds =
                ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Flux.empty();
        }
        String placeholders = java.util.stream.IntStream.range(0, distinctIds.size())
                .mapToObj(index -> ":id_" + index)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " p"
                        + " WHERE p.tenant_id=:tenant_id AND p.deleted=0 AND p.id IN (" + placeholders + ")"
                        + " ORDER BY p.id ASC")
                .bind("tenant_id", tenantId);
        for (int index = 0; index < distinctIds.size(); index++) {
            spec = spec.bind("id_" + index, distinctIds.get(index));
        }
        return spec.map(this::map).all();
    }

    @Override
    public Flux<ProfileBO> listByDeviceId(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) {
            return Flux.empty();
        }
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " p"
                        + " JOIN dc3_manager.dc3_device d ON d.profile_id=p.id AND d.id=:device_id"
                        + " AND d.tenant_id=p.tenant_id AND d.deleted=0"
                        + " WHERE p.tenant_id=:tenant_id AND p.deleted=0 ORDER BY p.id ASC")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<OffsetPage<ProfileBO>> list(ProfileFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("filter is required"));
        StringBuilder where = new StringBuilder(" WHERE p.tenant_id=:tenant_id AND p.deleted=0");
        if (present(filter.profileName())) where.append(" AND p.profile_name LIKE :profile_name");
        if (present(filter.profileCode())) where.append(" AND p.profile_code=:profile_code");
        if (filter.profileShareFlag() != null) where.append(" AND p.profile_share_flag=:profile_share_flag");
        if (filter.profileTypeFlag() != null) where.append(" AND p.profile_type_flag=:profile_type_flag");
        if (filter.enableFlag() != null) where.append(" AND p.enable_flag=:enable_flag");
        if (filter.version() != null) where.append(" AND p.version=:version");
        if (filter.deviceId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_device d"
                    + " WHERE d.id=:device_id AND d.profile_id=p.id AND d.tenant_id=p.tenant_id AND d.deleted=0)");
        }
        if (filter.groupId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_group_bind b"
                    + " WHERE b.deleted=0 AND b.tenant_id=p.tenant_id AND b.entity_id=p.id"
                    + " AND b.group_id=:group_id AND b.entity_type_flag=:entity_type)");
        }
        if (filter.labelId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM dc3_manager.dc3_label_bind b"
                    + " WHERE b.deleted=0 AND b.tenant_id=p.tenant_id AND b.entity_id=p.id"
                    + " AND b.label_id=:label_id AND b.entity_type_flag=:entity_type)");
        }

        DatabaseClient.GenericExecuteSpec countSpec =
                databaseClient.sql("SELECT COUNT(*) FROM " + TABLE + " p" + where);
        countSpec = bind(countSpec, filter);
        Mono<Long> total =
                countSpec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L);

        DatabaseClient.GenericExecuteSpec dataSpec = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + " p"
                + where + orderBy(filter.sort()) + " LIMIT :limit OFFSET :offset");
        dataSpec = bind(dataSpec, filter).bind("limit", filter.limit()).bind("offset", filter.offset());
        DatabaseClient.GenericExecuteSpec dataQuery = dataSpec;
        return total.flatMap(count -> dataQuery
                        .map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, filter.offset(), filter.limit(), count)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, ProfileFilter filter) {
        spec = spec.bind("tenant_id", filter.tenantId());
        if (present(filter.profileName()))
            spec = spec.bind("profile_name", "%" + filter.profileName().trim() + "%");
        if (present(filter.profileCode()))
            spec = spec.bind("profile_code", filter.profileCode().trim());
        if (filter.profileShareFlag() != null)
            spec = spec.bind("profile_share_flag", filter.profileShareFlag().getIndex());
        if (filter.profileTypeFlag() != null)
            spec = spec.bind("profile_type_flag", filter.profileTypeFlag().getIndex());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        if (filter.version() != null) spec = spec.bind("version", filter.version());
        if (filter.deviceId() != null) spec = spec.bind("device_id", filter.deviceId());
        if (filter.groupId() != null) spec = spec.bind("group_id", filter.groupId());
        if (filter.labelId() != null) spec = spec.bind("label_id", filter.labelId());
        if (filter.groupId() != null || filter.labelId() != null) {
            spec = spec.bind("entity_type", EntityTypeEnum.PROFILE.getIndex());
        }
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null) {
            for (SortSpec spec : sort) {
                String column =
                        switch (spec.field()) {
                            case "profileName" -> "p.profile_name";
                            case "profileCode" -> "p.profile_code";
                            case "createTime" -> "p.create_time";
                            case "operateTime" -> "p.operate_time";
                            case "version" -> "p.version";
                            case "id" -> "p.id";
                            default ->
                                throw new IllegalArgumentException("unsupported profile sort field: " + spec.field());
                        };
                clauses.add(column + (spec.direction() == SortSpec.Direction.DESC ? " DESC" : " ASC"));
            }
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("p.id"))) clauses.add("p.id ASC");
        return " ORDER BY " + String.join(", ", clauses);
    }

    private ProfileBO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        ProfileBO value = new ProfileBO();
        value.setId(row.get("id", Long.class));
        value.setProfileName(row.get("profile_name", String.class));
        value.setProfileCode(row.get("profile_code", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setRemark(row.get("remark", String.class));
        value.setSignature(row.get("signature", String.class));
        value.setVersion(row.get("version", Integer.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number share = row.get("profile_share_flag", Number.class);
        Number type = row.get("profile_type_flag", Number.class);
        Number enabled = row.get("enable_flag", Number.class);
        value.setProfileShareFlag(ProfileShareTypeEnum.ofIndex(share == null ? null : share.byteValue()));
        value.setProfileTypeFlag(ProfileTypeEnum.ofIndex(type == null ? null : type.byteValue()));
        value.setEnableFlag(EnableFlagEnum.ofIndex(enabled == null ? null : enabled.byteValue()));
        value.setProfileExt(profileExt(row.get("profile_ext", String.class)));
        return value;
    }

    private ProfileExt profileExt(String raw) {
        if (raw == null) return null;
        try {
            JsonExt json = objectMapper.readValue(raw, JsonExt.class);
            ProfileExt ext = new ProfileExt();
            ext.setType(json.getType());
            ext.setVersion(json.getVersion());
            ext.setRemark(json.getRemark());
            ext.setContent(
                    json.getContent() == null
                            ? null
                            : JsonUtil.parseObject(json.getContent(), ProfileExt.Content.class));
            return ext;
        } catch (Exception exception) {
            throw new IllegalStateException("profile_ext contains invalid JSON", exception);
        }
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private Byte index(ProfileShareTypeEnum value) {
        return value == null ? ProfileShareTypeEnum.TENANT.getIndex() : value.getIndex();
    }

    private Byte index(ProfileTypeEnum value) {
        return value == null ? ProfileTypeEnum.USER.getIndex() : value.getIndex();
    }

    private Byte index(EnableFlagEnum value) {
        return value == null ? EnableFlagEnum.ENABLE.getIndex() : value.getIndex();
    }

    private String serialize(ProfileExt value) {
        try {
            JsonExt json = new JsonExt();
            if (value != null) {
                json.setType(value.getType());
                json.setVersion(value.getVersion());
                json.setRemark(value.getRemark());
                json.setContent(value.getContent() == null ? null : JsonUtil.toJsonString(value.getContent()));
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("profile_ext is not valid JSON", exception);
        }
    }
}
