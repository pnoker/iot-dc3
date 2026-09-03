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
package io.github.pnoker.db.postgres.data;

import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;

import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for notification administration writes and lists. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class, PageTransaction.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcNotifyAdminStore implements ReactiveNotifyAdminStore {
    private static final String NOTIFY = "dc3_data.dc3_notify";
    private static final String MESSAGE = "dc3_data.dc3_message";
    private static final String CHANNEL = "dc3_data.dc3_notify_channel";
    private static final String BIND = "dc3_data.dc3_notify_channel_bind";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final PageTransaction pageTransaction;
    private final R2dbcDialect dialect;

    @Override
    public Mono<NotifyDO> getNotify(long tenantId, long id) {
        return one(
                NOTIFY,
                notifyColumns(),
                "tenant_id=:tenant_id AND id=:id AND deleted=0",
                List.of(param("id", id)),
                this::mapNotify,
                tenantId);
    }

    @Override
    public Mono<OffsetPage<NotifyDO>> listNotify(
            long tenantId,
            String name,
            String code,
            AutoConfirmFlagEnum autoConfirm,
            Long interval,
            EnableFlagEnum enable,
            PageRequest page) {
        List<Param> params = new ArrayList<>();
        String where = "tenant_id=:tenant_id AND deleted=0";
        if (name != null && !name.isBlank()) {
            where += " AND notify_name LIKE :name";
            params.add(param("name", "%" + name.trim() + "%"));
        }
        if (code != null && !code.isBlank()) {
            where += " AND notify_code=:code";
            params.add(param("code", code.trim()));
        }
        if (autoConfirm != null) {
            where += " AND auto_confirm_flag=:auto_confirm";
            params.add(param("auto_confirm", autoConfirm.getIndex()));
        }
        if (interval != null) {
            where += " AND notify_interval=:interval";
            params.add(param("interval", interval));
        }
        if (enable != null) {
            where += " AND enable_flag=:enable";
            params.add(param("enable", enable.getIndex()));
        }
        return page(
                NOTIFY,
                notifyColumns(),
                where,
                params,
                orderBy(page.sort(), "notify"),
                page,
                this::mapNotify,
                tenantId);
    }

    @Override
    public Mono<NotifyDO> insertNotify(NotifyDO value) {
        if (!valid(value, value == null ? null : value.getTenantId()))
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        prepare(value);
        if (value.getId() == null) value.setId(newId());
        String sql = "INSERT INTO " + NOTIFY
                + " (id,notify_name,notify_code,auto_confirm_flag,notify_interval,notify_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:name,:code,:auto_confirm,:interval,"
                + dialect.jsonWriteExpression(":ext")
                + ",:enable,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", value.getId())
                .bind("name", text(value.getNotifyName()))
                .bind("code", text(value.getNotifyCode()))
                .bind("auto_confirm", number(value.getAutoConfirmFlag()))
                .bind("interval", number(value.getNotifyInterval()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", text(value.getRemark()))
                .bind("creator_id", number(value.getCreatorId()))
                .bind("creator_name", text(value.getCreatorName()))
                .bind("create_time", value.getCreateTime())
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", value.getOperateTime());
        spec = nullable(spec, "ext", serialize(value.getNotifyExt()), String.class);
        return write(spec).flatMap(ok -> getNotify(value.getTenantId(), value.getId()));
    }

    @Override
    public Mono<NotifyDO> updateNotify(NotifyDO value) {
        if (!valid(value, value == null ? null : value.getTenantId())) return Mono.empty();
        String sql = "UPDATE " + NOTIFY
                + " SET notify_name=:name,notify_code=:code,auto_confirm_flag=:auto_confirm,notify_interval=:interval,notify_ext="
                + dialect.jsonWriteExpression(":ext")
                + ",enable_flag=:enable,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("name", text(value.getNotifyName()))
                .bind("code", text(value.getNotifyCode()))
                .bind("auto_confirm", number(value.getAutoConfirmFlag()))
                .bind("interval", number(value.getNotifyInterval()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("id", value.getId())
                .bind("remark", text(value.getRemark()))
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", utcNow());
        spec = nullable(spec, "ext", serialize(value.getNotifyExt()), String.class);
        return rows(spec)
                .flatMap(updated -> updated == 1 ? getNotify(value.getTenantId(), value.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteNotify(long tenantId, long id) {
        return delete(NOTIFY, tenantId, id);
    }

    @Override
    public Mono<Boolean> hasNotifyBindings(long tenantId, long notifyId) {
        return exists(BIND, tenantId, "notify_id", notifyId);
    }

    @Override
    public Mono<Boolean> existsNotifyCode(long tenantId, String code, Long excludedId) {
        return existsCode(NOTIFY, "notify_code", tenantId, code, excludedId);
    }

    @Override
    public Mono<MessageDO> getMessage(long tenantId, long id) {
        return one(
                MESSAGE,
                messageColumns(),
                "tenant_id=:tenant_id AND id=:id AND deleted=0",
                List.of(param("id", id)),
                this::mapMessage,
                tenantId);
    }

    @Override
    public Mono<OffsetPage<MessageDO>> listMessage(
            long tenantId,
            String name,
            String code,
            AlarmMessageLevelEnum level,
            EnableFlagEnum enable,
            PageRequest page) {
        List<Param> params = new ArrayList<>();
        String where = "tenant_id=:tenant_id AND deleted=0";
        if (name != null && !name.isBlank()) {
            where += " AND message_name LIKE :name";
            params.add(param("name", "%" + name.trim() + "%"));
        }
        if (code != null && !code.isBlank()) {
            where += " AND message_code=:code";
            params.add(param("code", code.trim()));
        }
        if (level != null) {
            where += " AND message_level=:level";
            params.add(param("level", level.getIndex()));
        }
        if (enable != null) {
            where += " AND enable_flag=:enable";
            params.add(param("enable", enable.getIndex()));
        }
        return page(
                MESSAGE,
                messageColumns(),
                where,
                params,
                orderBy(page.sort(), "message"),
                page,
                this::mapMessage,
                tenantId);
    }

    @Override
    public Mono<MessageDO> insertMessage(MessageDO value) {
        if (!valid(value, value == null ? null : value.getTenantId()))
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        prepare(value);
        if (value.getId() == null) value.setId(newId());
        String sql = "INSERT INTO " + MESSAGE
                + " (id,message_name,message_code,message_level,message_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:name,:code,:level,"
                + dialect.jsonWriteExpression(":ext")
                + ",:enable,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", value.getId())
                .bind("name", text(value.getMessageName()))
                .bind("code", text(value.getMessageCode()))
                .bind("level", number(value.getMessageLevel()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", text(value.getRemark()))
                .bind("creator_id", number(value.getCreatorId()))
                .bind("creator_name", text(value.getCreatorName()))
                .bind("create_time", value.getCreateTime())
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", value.getOperateTime());
        spec = nullable(spec, "ext", serialize(value.getMessageExt()), String.class);
        return write(spec).flatMap(ok -> getMessage(value.getTenantId(), value.getId()));
    }

    @Override
    public Mono<MessageDO> updateMessage(MessageDO value) {
        if (!valid(value, value == null ? null : value.getTenantId())) return Mono.empty();
        String sql =
                "UPDATE " + MESSAGE + " SET message_name=:name,message_code=:code,message_level=:level,message_ext="
                        + dialect.jsonWriteExpression(":ext")
                        + ",enable_flag=:enable,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("name", text(value.getMessageName()))
                .bind("code", text(value.getMessageCode()))
                .bind("level", number(value.getMessageLevel()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("id", value.getId())
                .bind("remark", text(value.getRemark()))
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", utcNow());
        spec = nullable(spec, "ext", serialize(value.getMessageExt()), String.class);
        return rows(spec)
                .flatMap(updated -> updated == 1 ? getMessage(value.getTenantId(), value.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteMessage(long tenantId, long id) {
        return delete(MESSAGE, tenantId, id);
    }

    @Override
    public Mono<Boolean> existsMessageCode(long tenantId, String code, Long excludedId) {
        return existsCode(MESSAGE, "message_code", tenantId, code, excludedId);
    }

    @Override
    public Mono<NotifyChannelDO> getChannel(long tenantId, long id) {
        return one(
                CHANNEL,
                channelColumns(),
                "tenant_id=:tenant_id AND id=:id AND deleted=0",
                List.of(param("id", id)),
                this::mapChannel,
                tenantId);
    }

    @Override
    public Mono<OffsetPage<NotifyChannelDO>> listChannel(
            long tenantId,
            String name,
            String code,
            NotifyChannelTypeEnum type,
            EnableFlagEnum enable,
            PageRequest page) {
        List<Param> params = new ArrayList<>();
        String where = "tenant_id=:tenant_id AND deleted=0";
        if (name != null && !name.isBlank()) {
            where += " AND channel_name LIKE :name";
            params.add(param("name", "%" + name.trim() + "%"));
        }
        if (code != null && !code.isBlank()) {
            where += " AND channel_code=:code";
            params.add(param("code", code.trim()));
        }
        if (type != null) {
            where += " AND channel_type_flag=:type";
            params.add(param("type", type.getIndex()));
        }
        if (enable != null) {
            where += " AND enable_flag=:enable";
            params.add(param("enable", enable.getIndex()));
        }
        return page(
                CHANNEL,
                channelColumns(),
                where,
                params,
                orderBy(page.sort(), "channel"),
                page,
                this::mapChannel,
                tenantId);
    }

    @Override
    public Mono<NotifyChannelDO> insertChannel(NotifyChannelDO value) {
        if (!valid(value, value == null ? null : value.getTenantId()))
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        prepare(value);
        if (value.getId() == null) value.setId(newId());
        String sql = "INSERT INTO " + CHANNEL
                + " (id,channel_name,channel_code,channel_type_flag,credential_ref,channel_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:name,:code,:type,:credential,"
                + dialect.jsonWriteExpression(":ext")
                + ",:enable,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", value.getId())
                .bind("name", text(value.getChannelName()))
                .bind("code", text(value.getChannelCode()))
                .bind("type", number(value.getChannelTypeFlag()))
                .bind("credential", text(value.getCredentialRef()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", text(value.getRemark()))
                .bind("creator_id", number(value.getCreatorId()))
                .bind("creator_name", text(value.getCreatorName()))
                .bind("create_time", value.getCreateTime())
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", value.getOperateTime());
        spec = nullable(spec, "ext", serialize(value.getChannelExt()), String.class);
        return write(spec).flatMap(ok -> getChannel(value.getTenantId(), value.getId()));
    }

    @Override
    public Mono<NotifyChannelDO> updateChannel(NotifyChannelDO value) {
        if (!valid(value, value == null ? null : value.getTenantId())) return Mono.empty();
        String sql = "UPDATE " + CHANNEL
                + " SET channel_name=:name,channel_code=:code,channel_type_flag=:type,credential_ref=:credential,channel_ext="
                + dialect.jsonWriteExpression(":ext")
                + ",enable_flag=:enable,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("name", text(value.getChannelName()))
                .bind("code", text(value.getChannelCode()))
                .bind("type", number(value.getChannelTypeFlag()))
                .bind("credential", text(value.getCredentialRef()))
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("id", value.getId())
                .bind("remark", text(value.getRemark()))
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", utcNow());
        spec = nullable(spec, "ext", serialize(value.getChannelExt()), String.class);
        return rows(spec)
                .flatMap(updated -> updated == 1 ? getChannel(value.getTenantId(), value.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteChannel(long tenantId, long id) {
        return delete(CHANNEL, tenantId, id);
    }

    @Override
    public Mono<Boolean> hasChannelBindings(long tenantId, long channelId) {
        return exists(BIND, tenantId, "channel_id", channelId);
    }

    @Override
    public Mono<Boolean> existsChannelCode(long tenantId, String code, Long excludedId) {
        return existsCode(CHANNEL, "channel_code", tenantId, code, excludedId);
    }

    @Override
    public Mono<NotifyChannelBindDO> getBind(long tenantId, long id) {
        return one(
                BIND,
                bindColumns(),
                "tenant_id=:tenant_id AND id=:id AND deleted=0",
                List.of(param("id", id)),
                this::mapBind,
                tenantId);
    }

    @Override
    public Mono<OffsetPage<NotifyChannelBindDO>> listBind(
            long tenantId, Long notifyId, Long channelId, EnableFlagEnum enable, PageRequest page) {
        List<Param> params = new ArrayList<>();
        String where = "tenant_id=:tenant_id AND deleted=0";
        if (notifyId != null) {
            where += " AND notify_id=:notify_id";
            params.add(param("notify_id", notifyId));
        }
        if (channelId != null) {
            where += " AND channel_id=:channel_id";
            params.add(param("channel_id", channelId));
        }
        if (enable != null) {
            where += " AND enable_flag=:enable";
            params.add(param("enable", enable.getIndex()));
        }
        return page(BIND, bindColumns(), where, params, orderBy(page.sort(), "bind"), page, this::mapBind, tenantId);
    }

    @Override
    public Mono<NotifyChannelBindDO> insertBind(NotifyChannelBindDO value) {
        if (!valid(value, value == null ? null : value.getTenantId()))
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        prepare(value);
        if (value.getId() == null) value.setId(newId());
        String sql = "INSERT INTO " + BIND
                + " (id,notify_id,channel_id,bind_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted) VALUES (:id,:notify_id,:channel_id,"
                + dialect.jsonWriteExpression(":ext")
                + ",:enable,:tenant_id,:remark,:creator_id,:creator_name,:create_time,:operator_id,:operator_name,:operate_time,0)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("id", value.getId())
                .bind("notify_id", value.getNotifyId())
                .bind("channel_id", value.getChannelId())
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("remark", text(value.getRemark()))
                .bind("creator_id", number(value.getCreatorId()))
                .bind("creator_name", text(value.getCreatorName()))
                .bind("create_time", value.getCreateTime())
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", value.getOperateTime());
        spec = nullable(spec, "ext", serialize(value.getBindExt()), String.class);
        return write(spec).flatMap(ok -> getBind(value.getTenantId(), value.getId()));
    }

    @Override
    public Mono<NotifyChannelBindDO> updateBind(NotifyChannelBindDO value) {
        if (!valid(value, value == null ? null : value.getTenantId())) return Mono.empty();
        String sql = "UPDATE " + BIND + " SET notify_id=:notify_id,channel_id=:channel_id,bind_ext="
                + dialect.jsonWriteExpression(":ext")
                + ",enable_flag=:enable,remark=:remark,operator_id=:operator_id,operator_name=:operator_name,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0";
        DatabaseClient.GenericExecuteSpec spec = databaseClient
                .sql(sql)
                .bind("notify_id", value.getNotifyId())
                .bind("channel_id", value.getChannelId())
                .bind("enable", number(value.getEnableFlag()))
                .bind("tenant_id", value.getTenantId())
                .bind("id", value.getId())
                .bind("remark", text(value.getRemark()))
                .bind("operator_id", number(value.getOperatorId()))
                .bind("operator_name", text(value.getOperatorName()))
                .bind("operate_time", utcNow());
        spec = nullable(spec, "ext", serialize(value.getBindExt()), String.class);
        return rows(spec).flatMap(updated -> updated == 1 ? getBind(value.getTenantId(), value.getId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> deleteBind(long tenantId, long id) {
        return delete(BIND, tenantId, id);
    }

    @Override
    public Mono<Boolean> existsBind(long tenantId, long notifyId, long channelId, Long excludedId) {
        String where = "tenant_id=:tenant_id AND notify_id=:notify_id AND channel_id=:channel_id AND deleted=0"
                + (excludedId == null ? "" : " AND id<>:excluded_id");
        List<Param> params = new ArrayList<>(List.of(param("notify_id", notifyId), param("channel_id", channelId)));
        if (excludedId != null) params.add(param("excluded_id", excludedId));
        return scalar(BIND, where, params, tenantId);
    }

    @Override
    public Mono<Boolean> existsNotify(long tenantId, long id) {
        return scalar(NOTIFY, "tenant_id=:tenant_id AND id=:id AND deleted=0", List.of(param("id", id)), tenantId);
    }

    @Override
    public Mono<Boolean> existsChannel(long tenantId, long id) {
        return scalar(CHANNEL, "tenant_id=:tenant_id AND id=:id AND deleted=0", List.of(param("id", id)), tenantId);
    }

    private <T> Mono<T> one(
            String table,
            String columns,
            String where,
            List<Param> params,
            BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, T> mapper,
            long tenantId) {
        return bind(
                        databaseClient
                                .sql("SELECT " + columns + " FROM " + table + " WHERE " + where + " LIMIT 1")
                                .bind("tenant_id", tenantId),
                        params)
                .map(mapper)
                .one();
    }

    private <T> Mono<OffsetPage<T>> page(
            String table,
            String columns,
            String where,
            List<Param> params,
            String order,
            PageRequest request,
            BiFunction<io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata, T> mapper,
            long tenantId) {
        if (!valid(tenantId)) return Mono.just(OffsetPage.of(List.of(), request.offset(), request.limit(), 0));
        DatabaseClient.GenericExecuteSpec count = bind(
                databaseClient
                        .sql("SELECT COUNT(*) AS total FROM " + table + " WHERE " + where)
                        .bind("tenant_id", tenantId),
                params);
        DatabaseClient.GenericExecuteSpec rows = bind(
                databaseClient
                        .sql("SELECT " + columns + " FROM " + table + " WHERE " + where + " ORDER BY " + order
                                + " LIMIT :limit OFFSET :offset")
                        .bind("tenant_id", tenantId)
                        .bind("limit", request.limit())
                        .bind("offset", request.offset()),
                params);
        Mono<Long> total = count.map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                })
                .one()
                .defaultIfEmpty(0L);
        return total.flatMap(totalCount -> rows.map(mapper)
                        .all()
                        .collectList()
                        .map(items -> OffsetPage.of(items, request.offset(), request.limit(), totalCount)))
                .as(pageTransaction::transactional);
    }

    private Mono<Boolean> scalar(String table, String where, List<Param> params, long tenantId) {
        return bind(
                        databaseClient
                                .sql("SELECT 1 FROM " + table + " WHERE " + where + " LIMIT 1")
                                .bind("tenant_id", tenantId),
                        params)
                .map((row, metadata) -> true)
                .one()
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> exists(String table, long tenantId, String column, long id) {
        return scalar(
                table,
                "tenant_id=:tenant_id AND " + column + "=:value AND deleted=0",
                List.of(param("value", id)),
                tenantId);
    }

    private Mono<Boolean> existsCode(String table, String column, long tenantId, String code, Long excludedId) {
        if (code == null || code.isBlank()) return Mono.just(false);
        String where = "tenant_id=:tenant_id AND " + column + "=:code AND deleted=0"
                + (excludedId == null ? "" : " AND id<>:excluded_id");
        List<Param> params = new ArrayList<>(List.of(param("code", code.trim())));
        if (excludedId != null) params.add(param("excluded_id", excludedId));
        return scalar(table, where, params, tenantId);
    }

    private Mono<Boolean> delete(String table, long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.just(false);
        return transactionalOperator
                .transactional(databaseClient
                        .sql(
                                "UPDATE " + table
                                        + " SET deleted=1,operate_time=:operate_time WHERE tenant_id=:tenant_id AND id=:id AND deleted=0")
                        .bind("operate_time", utcNow())
                        .bind("tenant_id", tenantId)
                        .bind("id", id)
                        .fetch()
                        .rowsUpdated())
                .map(rows -> rows == 1);
    }

    private Mono<Boolean> write(DatabaseClient.GenericExecuteSpec spec) {
        return transactionalOperator.transactional(spec.fetch().rowsUpdated()).map(rows -> rows == 1);
    }

    private Mono<Long> rows(DatabaseClient.GenericExecuteSpec spec) {
        return transactionalOperator.transactional(spec.fetch().rowsUpdated());
    }

    private DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, List<Param> params) {
        for (Param value : params)
            spec = value.value() == null
                    ? spec.bindNull(value.name(), value.type())
                    : spec.bind(value.name(), value.value());
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec nullable(
            DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private Param param(String name, Object value) {
        return new Param(name, value, value == null ? String.class : value.getClass());
    }

    private record Param(String name, Object value, Class<?> type) {}

    private String notifyColumns() {
        return "id,notify_name,notify_code,auto_confirm_flag,notify_interval,notify_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    }

    private String messageColumns() {
        return "id,message_name,message_code,message_level,message_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    }

    private String channelColumns() {
        return "id,channel_name,channel_code,channel_type_flag,credential_ref,channel_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    }

    private String bindColumns() {
        return "id,notify_id,channel_id,bind_ext,enable_flag,tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted";
    }

    private String orderBy(List<SortSpec> sort, String kind) {
        if (sort == null || sort.isEmpty()) return "create_time DESC,id DESC";
        List<String> values = new ArrayList<>();
        for (SortSpec spec : sort) {
            String column =
                    switch (kind + ":" + spec.field()) {
                        case "notify:notifyName" -> "notify_name";
                        case "notify:notifyCode" -> "notify_code";
                        case "notify:notifyInterval" -> "notify_interval";
                        case "message:messageName" -> "message_name";
                        case "message:messageCode" -> "message_code";
                        case "message:messageLevel" -> "message_level";
                        case "channel:channelName" -> "channel_name";
                        case "channel:channelCode" -> "channel_code";
                        case "channel:channelTypeFlag" -> "channel_type_flag";
                        case "bind:notifyId" -> "notify_id";
                        case "bind:channelId" -> "channel_id";
                        case "notify:enableFlag", "message:enableFlag", "channel:enableFlag", "bind:enableFlag" ->
                            "enable_flag";
                        case "notify:createTime", "message:createTime", "channel:createTime", "bind:createTime" ->
                            "create_time";
                        case "notify:operateTime", "message:operateTime", "channel:operateTime", "bind:operateTime" ->
                            "operate_time";
                        case "notify:id", "message:id", "channel:id", "bind:id" -> "id";
                        default ->
                            throw new IllegalArgumentException("unsupported notification sort field: " + spec.field());
                    };
            values.add(column + (spec.direction() == SortSpec.Direction.ASC ? " ASC" : " DESC"));
        }
        if (values.stream().noneMatch(value -> value.startsWith("id "))) values.add("id DESC");
        return String.join(",", values);
    }

    private NotifyDO mapNotify(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        NotifyDO value = new NotifyDO();
        value.setId(longValue(row.get("id")));
        value.setNotifyName(text(row.get("notify_name")));
        value.setNotifyCode(text(row.get("notify_code")));
        value.setAutoConfirmFlag(byteValue(row.get("auto_confirm_flag")));
        value.setNotifyInterval(longValue(row.get("notify_interval")));
        value.setNotifyExt(parseJson(row.get("notify_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        common(value, row);
        return value;
    }

    private MessageDO mapMessage(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        MessageDO value = new MessageDO();
        value.setId(longValue(row.get("id")));
        value.setMessageName(text(row.get("message_name")));
        value.setMessageCode(text(row.get("message_code")));
        value.setMessageLevel(byteValue(row.get("message_level")));
        value.setMessageExt(parseJson(row.get("message_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        common(value, row);
        return value;
    }

    private NotifyChannelDO mapChannel(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        NotifyChannelDO value = new NotifyChannelDO();
        value.setId(longValue(row.get("id")));
        value.setChannelName(text(row.get("channel_name")));
        value.setChannelCode(text(row.get("channel_code")));
        value.setChannelTypeFlag(byteValue(row.get("channel_type_flag")));
        value.setCredentialRef(text(row.get("credential_ref")));
        value.setChannelExt(parseJson(row.get("channel_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        common(value, row);
        return value;
    }

    private NotifyChannelBindDO mapBind(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        NotifyChannelBindDO value = new NotifyChannelBindDO();
        value.setId(longValue(row.get("id")));
        value.setNotifyId(longValue(row.get("notify_id")));
        value.setChannelId(longValue(row.get("channel_id")));
        value.setBindExt(parseJson(row.get("bind_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        common(value, row);
        return value;
    }

    private void common(Object value, io.r2dbc.spi.Row row) {
        if (value instanceof NotifyDO v) commonFields(v, row);
        if (value instanceof MessageDO v) commonFields(v, row);
        if (value instanceof NotifyChannelDO v) commonFields(v, row);
        if (value instanceof NotifyChannelBindDO v) commonFields(v, row);
    }

    private void commonFields(NotifyDO value, io.r2dbc.spi.Row row) {
        value.setTenantId(longValue(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(longValue(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(longValue(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
    }

    private void commonFields(MessageDO value, io.r2dbc.spi.Row row) {
        value.setTenantId(longValue(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(longValue(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(longValue(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
    }

    private void commonFields(NotifyChannelDO value, io.r2dbc.spi.Row row) {
        value.setTenantId(longValue(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(longValue(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(longValue(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
    }

    private void commonFields(NotifyChannelBindDO value, io.r2dbc.spi.Row row) {
        value.setTenantId(longValue(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(longValue(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(longValue(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
    }

    private JsonExt parseJson(String value) {
        return value == null ? null : JsonUtil.parseObject(value, JsonExt.class);
    }

    private String serialize(Object value) {
        return value == null ? null : JsonUtil.toJsonString(value);
    }

    private long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private Byte byteValue(Object value) {
        return value instanceof Number n ? n.byteValue() : null;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime v) return v;
        if (value instanceof OffsetDateTime v)
            return v.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof Instant v) return LocalDateTime.ofInstant(v, ZoneOffset.UTC);
        return null;
    }

    private void prepare(Object value) {
        LocalDateTime now = utcNow();
        if (value instanceof NotifyDO v) {
            if (v.getCreateTime() == null) v.setCreateTime(now);
            if (v.getOperateTime() == null) v.setOperateTime(now);
        } else if (value instanceof MessageDO v) {
            if (v.getCreateTime() == null) v.setCreateTime(now);
            if (v.getOperateTime() == null) v.setOperateTime(now);
        } else if (value instanceof NotifyChannelDO v) {
            if (v.getCreateTime() == null) v.setCreateTime(now);
            if (v.getOperateTime() == null) v.setOperateTime(now);
        } else if (value instanceof NotifyChannelBindDO v) {
            if (v.getCreateTime() == null) v.setCreateTime(now);
            if (v.getOperateTime() == null) v.setOperateTime(now);
        }
    }

    private boolean valid(Object value, Long tenantId) {
        return value != null && tenantId != null && tenantId > 0;
    }

    private boolean valid(long value) {
        return value > 0;
    }

    private long newId() {
        return UuidV7.nextLong();
    }

    private long number(Long value) {
        return value == null ? 0L : value;
    }

    private byte number(Byte value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
