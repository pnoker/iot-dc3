package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Explicit SQL adapter for tenant-scoped users joined through active memberships. */
@Repository
@ConditionalOnClass({DatabaseClient.class, PageTransaction.class})
@RequiredArgsConstructor
public class R2dbcUserStore implements ReactiveUserStore {

    private static final String TABLE = "dc3_auth.dc3_user";
    private static final String MEMBERSHIP = "dc3_auth.dc3_tenant_membership";
    private static final String COLUMNS = "u.id,u.principal_id,u.user_name,u.nick_name,u.phone,u.email,u.social_ext,"
            + "u.identity_ext,u.enable_flag,u.remark,u.creator_id,u.creator_name,u.create_time,u.operator_id,"
            + "u.operator_name,u.operate_time,u.deleted";

    private final DatabaseClient databaseClient;
    private final PageTransaction pageTransaction;

    @Override
    public Mono<UserDO> getById(Long tenantId, Long id) {
        if (!valid(tenantId, id)) return Mono.empty();
        return query(COLUMNS + " FROM " + TABLE + " u WHERE u.id=:id AND u.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=u.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("id", id).map(this::map).one();
    }

    @Override
    public Mono<UserDO> getByUserName(Long tenantId, String userName) {
        if (!valid(tenantId) || userName == null || userName.isBlank()) return Mono.empty();
        return query(COLUMNS + " FROM " + TABLE + " u WHERE u.user_name=:user_name AND u.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=u.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("user_name", userName.trim()).map(this::map).one();
    }

    @Override
    public Mono<UserDO> getByPrincipalId(Long tenantId, Long principalId) {
        if (!valid(tenantId, principalId)) return Mono.empty();
        return query(COLUMNS + " FROM " + TABLE + " u WHERE u.principal_id=:principal_id AND u.deleted=0"
                        + " AND EXISTS (SELECT 1 FROM " + MEMBERSHIP + " m WHERE m.tenant_id=:tenant_id"
                        + " AND m.principal_id=u.principal_id AND m.membership_status='ACTIVE' AND m.deleted=0) LIMIT 1")
                .bind("tenant_id", tenantId).bind("principal_id", principalId).map(this::map).one();
    }

    @Override
    public Mono<OffsetPage<UserDO>> list(UserFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("user filter is required"));
        StringBuilder predicate = new StringBuilder(" FROM ").append(TABLE).append(" u WHERE u.deleted=0")
                .append(" AND EXISTS (SELECT 1 FROM ").append(MEMBERSHIP)
                .append(" m WHERE m.tenant_id=:tenant_id AND m.principal_id=u.principal_id")
                .append(" AND m.membership_status='ACTIVE' AND m.deleted=0)");
        if (filter.principalId() != null) predicate.append(" AND u.principal_id=:principal_id");
        if (filter.nickName() != null) predicate.append(" AND u.nick_name LIKE :nick_name");
        if (filter.userName() != null) predicate.append(" AND u.user_name LIKE :user_name");
        if (filter.phone() != null) predicate.append(" AND u.phone LIKE :phone");
        if (filter.email() != null) predicate.append(" AND u.email LIKE :email");
        if (filter.enableFlag() != null) predicate.append(" AND u.enable_flag=:enable_flag");
        DatabaseClient.GenericExecuteSpec count = bind(databaseClient.sql("SELECT COUNT(*) AS total" + predicate), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(databaseClient.sql("SELECT " + COLUMNS + predicate
                + " ORDER BY " + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"), filter)
                .bind("limit", filter.page().limit()).bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> {
            Number value = row.get("total", Number.class);
            return value == null ? 0L : value.longValue();
        }).one().defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map).all().collectList()
                        .map(items -> OffsetPage.of(items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec query, UserFilter filter) {
        query = query.bind("tenant_id", filter.tenantId());
        if (filter.principalId() != null) query = query.bind("principal_id", filter.principalId());
        if (filter.nickName() != null) query = query.bind("nick_name", "%" + filter.nickName() + "%");
        if (filter.userName() != null) query = query.bind("user_name", "%" + filter.userName() + "%");
        if (filter.phone() != null) query = query.bind("phone", "%" + filter.phone() + "%");
        if (filter.email() != null) query = query.bind("email", "%" + filter.email() + "%");
        if (filter.enableFlag() != null) query = query.bind("enable_flag", filter.enableFlag().getIndex());
        return query;
    }

    private DatabaseClient.GenericExecuteSpec query(String suffix) {
        return databaseClient.sql("SELECT " + suffix);
    }

    private UserDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        UserDO value = new UserDO();
        value.setId(row.get("id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setUserName(row.get("user_name", String.class));
        value.setNickName(row.get("nick_name", String.class));
        value.setPhone(row.get("phone", String.class));
        value.setEmail(row.get("email", String.class));
        value.setSocialExt(json(row.get("social_ext", String.class)));
        value.setIdentityExt(json(row.get("identity_ext", String.class)));
        Number enable = row.get("enable_flag", Number.class);
        value.setEnableFlag(enable == null ? null : enable.byteValue());
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(row.get("operator_id", Long.class));
        value.setOperatorName(row.get("operator_name", String.class));
        value.setOperateTime(time(row.get("operate_time")));
        Number deleted = row.get("deleted", Number.class);
        value.setDeleted(deleted == null ? null : deleted.byteValue());
        return value;
    }

    private JsonExt json(String raw) {
        if (raw == null) return null;
        try { return JsonUtil.parseObject(raw, JsonExt.class); } catch (RuntimeException ignored) { return null; }
    }

    private LocalDateTime time(Object raw) {
        if (raw instanceof LocalDateTime value) return value;
        if (raw instanceof OffsetDateTime value) return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (raw instanceof java.time.Instant value) return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        return null;
    }

    private String orderBy(List<SortSpec> sort) {
        if (sort == null || sort.isEmpty()) return "u.user_name ASC,u.id ASC";
        List<String> clauses = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column = switch (spec.field()) {
                case "id" -> "u.id"; case "userName" -> "u.user_name"; case "nickName" -> "u.nick_name";
                case "phone" -> "u.phone"; case "email" -> "u.email"; case "createTime" -> "u.create_time";
                case "operateTime" -> "u.operate_time"; default -> throw new IllegalArgumentException("unsupported user sort field: " + spec.field());
            };
            clauses.add(column + " " + spec.direction().name());
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("u.id "))) clauses.add("u.id ASC");
        return String.join(",", clauses);
    }

    private boolean valid(Long tenantId) { return tenantId != null && tenantId > 0; }
    private boolean valid(Long tenantId, Long id) { return valid(tenantId) && id != null && id > 0; }
}
