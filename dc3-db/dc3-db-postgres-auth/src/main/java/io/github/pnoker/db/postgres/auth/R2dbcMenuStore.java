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

import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.common.auth.repository.ReactiveMenuStore;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MenuLevelEnum;
import io.github.pnoker.common.enums.MenuTypeFlagEnum;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcMenuStore implements ReactiveMenuStore {

    private static final String TABLE = "dc3_auth.dc3_menu";
    private static final String COLUMNS = "id,parent_menu_id,menu_type_flag,menu_name,menu_code,menu_level,"
            + "menu_index,menu_ext,enable_flag,remark,creator_id,creator_name,create_time,operator_id,"
            + "operator_name,operate_time,deleted";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<MenuDO> getById(Long id) {
        if (!valid(id)) return Mono.empty();
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .map(this::map)
                .one();
    }

    @Override
    public Mono<OffsetPage<MenuDO>> list(MenuFilter filter) {
        if (filter == null) return Mono.error(new IllegalArgumentException("menu filter is required"));
        String where = whereClause(filter);
        DatabaseClient.GenericExecuteSpec count =
                bind(databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + where), filter);
        DatabaseClient.GenericExecuteSpec rows = bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + where + " ORDER BY "
                                + orderBy(filter.page().sort()) + " LIMIT :limit OFFSET :offset"),
                        filter)
                .bind("limit", filter.page().limit())
                .bind("offset", filter.page().offset());
        Mono<Long> total = count.map((row, metadata) -> Optional.ofNullable(row.get("total", Number.class))
                        .map(Number::longValue)
                        .orElse(0L))
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(this::map)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(
                                items, filter.page().offset(), filter.page().limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    @Override
    public Flux<MenuDO> listTree(MenuFilter filter) {
        if (filter == null) return Flux.error(new IllegalArgumentException("menu filter is required"));
        return bind(
                        databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE + whereClause(filter) + " ORDER BY "
                                + orderBy(filter.page().sort())),
                        filter)
                .map(this::map)
                .all();
    }

    @Override
    public Mono<MenuDO> insert(MenuBO menu) {
        if (menu == null) return Mono.error(new IllegalArgumentException("menu is required"));
        long id = UuidV7.nextLong();
        LocalDateTime now = now();
        String sql = "INSERT INTO " + TABLE + " (id,parent_menu_id,menu_type_flag,menu_name,menu_code,menu_level,"
                + "menu_index,menu_ext,enable_flag,remark,creator_id,creator_name,create_time,operator_id,"
                + "operator_name,operate_time,deleted) VALUES (:id,:parent_menu_id,:menu_type_flag,:menu_name,"
                + ":menu_code,:menu_level,:menu_index," + dialect.jsonWriteExpression(":menu_ext")
                + ",:enable_flag,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", id)
                .bind("parent_menu_id", value(menu.getParentMenuId()))
                .bind("menu_type_flag", index(menu.getMenuTypeFlag()))
                .bind("menu_name", text(menu.getMenuName()))
                .bind("menu_code", text(menu.getMenuCode()))
                .bind("menu_level", index(menu.getMenuLevel()))
                .bind("menu_index", value(menu.getMenuIndex()))
                .bind("menu_ext", json(menu.getMenuExt()))
                .bind("enable_flag", index(menu.getEnableFlag()))
                .bind("remark", text(menu.getRemark()))
                .bind("creator_id", value(menu.getCreatorId()))
                .bind("creator_name", text(menu.getCreatorName()))
                .bind("create_time", now)
                .bind("operator_id", value(menu.getOperatorId()))
                .bind("operator_name", text(menu.getOperatorName()))
                .bind("operate_time", now);
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(id));
    }

    @Override
    public Mono<MenuDO> update(MenuBO menu) {
        if (menu == null || !valid(menu.getId())) return Mono.empty();
        String sql = "UPDATE " + TABLE + " SET parent_menu_id=:parent_menu_id,menu_type_flag=:menu_type_flag,"
                + "menu_name=:menu_name,menu_code=:menu_code,menu_level=:menu_level,menu_index=:menu_index,"
                + "menu_ext=" + dialect.jsonWriteExpression(":menu_ext")
                + ",enable_flag=:enable_flag,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,"
                + "operate_time=:operate_time WHERE id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql(sql)
                .bind("id", menu.getId())
                .bind("parent_menu_id", value(menu.getParentMenuId()))
                .bind("menu_type_flag", index(menu.getMenuTypeFlag()))
                .bind("menu_name", text(menu.getMenuName()))
                .bind("menu_code", text(menu.getMenuCode()))
                .bind("menu_level", index(menu.getMenuLevel()))
                .bind("menu_index", value(menu.getMenuIndex()))
                .bind("menu_ext", json(menu.getMenuExt()))
                .bind("enable_flag", index(menu.getEnableFlag()))
                .bind("remark", text(menu.getRemark()))
                .bind("operator_id", value(menu.getOperatorId()))
                .bind("operator_name", text(menu.getOperatorName()))
                .bind("operate_time", now());
        return transactionalOperator.transactional(query.fetch().rowsUpdated()).then(getById(menu.getId()));
    }

    @Override
    public Mono<Boolean> delete(Long id, Long operatorId, String operatorName) {
        if (!valid(id)) return Mono.just(false);
        return transactionalOperator
                .transactional(databaseClient
                        .sql("UPDATE " + TABLE
                                + " SET deleted=1,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time"
                                + " WHERE id=:id AND deleted=0")
                        .bind("id", id)
                        .bind("operator_id", value(operatorId))
                        .bind("operator_name", text(operatorName))
                        .bind("operate_time", now())
                        .fetch()
                        .rowsUpdated())
                .map(updated -> updated == 1);
    }

    @Override
    public Mono<Boolean> existsDuplicate(MenuBO menu) {
        if (menu == null || isBlank(menu.getMenuCode())) return Mono.just(false);
        String predicate = " WHERE menu_code=:menu_code AND deleted=0";
        if (menu.getId() != null) predicate += " AND id<>:id";
        DatabaseClient.GenericExecuteSpec query = databaseClient
                .sql("SELECT 1 FROM " + TABLE + predicate + " LIMIT 1")
                .bind("menu_code", menu.getMenuCode().trim());
        if (menu.getId() != null) query = query.bind("id", menu.getId());
        return query.fetch().first().hasElement();
    }

    @Override
    public Mono<Boolean> hasChildren(Long id) {
        if (!valid(id)) return Mono.just(false);
        return databaseClient
                .sql("SELECT 1 FROM " + TABLE + " WHERE parent_menu_id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .fetch()
                .first()
                .hasElement();
    }

    @Override
    public Mono<Boolean> isDescendant(Long rootId, Long candidateId) {
        if (!valid(rootId) || !valid(candidateId)) return Mono.just(false);
        String sql = "WITH RECURSIVE descendants(id) AS (SELECT id FROM " + TABLE
                + " WHERE parent_menu_id=:root_id AND deleted=0 UNION SELECT menu.id FROM " + TABLE
                + " menu JOIN descendants parent ON menu.parent_menu_id=parent.id WHERE menu.deleted=0)"
                + " SELECT 1 FROM descendants WHERE id=:candidate_id LIMIT 1";
        return databaseClient
                .sql(sql)
                .bind("root_id", rootId)
                .bind("candidate_id", candidateId)
                .fetch()
                .first()
                .hasElement();
    }

    private String whereClause(MenuFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE deleted=0");
        if (filter.parentMenuId() != null) where.append(" AND parent_menu_id=:parent_menu_id");
        if (filter.menuTypeFlag() != null) where.append(" AND menu_type_flag=:menu_type_flag");
        if (filter.menuName() != null)
            where.append(" AND ").append(dialect.caseInsensitiveLike("menu_name", ":menu_name"));
        if (filter.menuCode() != null) where.append(" AND menu_code=:menu_code");
        if (filter.enableFlag() != null) where.append(" AND enable_flag=:enable_flag");
        return where.toString();
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, MenuFilter filter) {
        if (filter.parentMenuId() != null) spec = spec.bind("parent_menu_id", filter.parentMenuId());
        if (filter.menuTypeFlag() != null)
            spec = spec.bind("menu_type_flag", filter.menuTypeFlag().getIndex());
        if (filter.menuName() != null) spec = spec.bind("menu_name", "%" + filter.menuName() + "%");
        if (filter.menuCode() != null) spec = spec.bind("menu_code", filter.menuCode());
        if (filter.enableFlag() != null)
            spec = spec.bind("enable_flag", filter.enableFlag().getIndex());
        return spec;
    }

    private String orderBy(List<SortSpec> sort) {
        List<String> clauses = new ArrayList<>();
        if (sort != null) {
            for (SortSpec spec : sort) {
                String column =
                        switch (spec.field()) {
                            case "id" -> "id";
                            case "parentMenuId" -> "parent_menu_id";
                            case "menuName" -> "menu_name";
                            case "menuCode" -> "menu_code";
                            case "menuIndex" -> "menu_index";
                            case "menuTypeFlag" -> "menu_type_flag";
                            case "enableFlag" -> "enable_flag";
                            default ->
                                throw new IllegalArgumentException("unsupported menu sort field: " + spec.field());
                        };
                clauses.add(column + " " + spec.direction().name());
            }
        }
        if (clauses.stream().noneMatch(value -> value.startsWith("id "))) clauses.add("id ASC");
        return String.join(",", clauses);
    }

    private MenuDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        MenuDO menu = new MenuDO();
        menu.setId(row.get("id", Long.class));
        menu.setParentMenuId(row.get("parent_menu_id", Long.class));
        menu.setMenuTypeFlag(number(row.get("menu_type_flag")));
        menu.setMenuName(row.get("menu_name", String.class));
        menu.setMenuCode(row.get("menu_code", String.class));
        menu.setMenuLevel(number(row.get("menu_level")));
        menu.setMenuIndex(number(row.get("menu_index")));
        menu.setMenuExt(parseJson(row.get("menu_ext", String.class)));
        menu.setEnableFlag(number(row.get("enable_flag")));
        menu.setRemark(row.get("remark", String.class));
        menu.setCreatorId(row.get("creator_id", Long.class));
        menu.setCreatorName(row.get("creator_name", String.class));
        menu.setCreateTime(time(row.get("create_time")));
        menu.setOperatorId(row.get("operator_id", Long.class));
        menu.setOperatorName(row.get("operator_name", String.class));
        menu.setOperateTime(time(row.get("operate_time")));
        menu.setDeleted(number(row.get("deleted")));
        return menu;
    }

    private JsonExt parseJson(String value) {
        if (value == null) return null;
        return JsonUtil.parseObject(value, JsonExt.class);
    }

    private Byte number(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    private String json(Object value) {
        return value == null ? "{}" : JsonUtil.toJsonString(value);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private short value(Integer value) {
        return value == null ? 0 : value.shortValue();
    }

    private byte index(Object value) {
        if (value instanceof EnableFlagEnum flag) return flag.getIndex();
        if (value instanceof MenuTypeFlagEnum flag) return flag.getIndex();
        if (value instanceof MenuLevelEnum flag) return flag.getIndex();
        return 0;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean valid(Long id) {
        return id != null && id > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
