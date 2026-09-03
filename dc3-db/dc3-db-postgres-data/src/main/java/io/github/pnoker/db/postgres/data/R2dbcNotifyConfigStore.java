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

import io.github.pnoker.common.data.repository.ReactiveNotifyConfigStore;

import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Explicit SQL adapter for notification configuration reads. */
@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcNotifyConfigStore implements ReactiveNotifyConfigStore {

    private static final String NOTIFY_TABLE = "dc3_data.dc3_notify";
    private static final String MESSAGE_TABLE = "dc3_data.dc3_message";
    private static final String CHANNEL_TABLE = "dc3_data.dc3_notify_channel";
    private static final String BIND_TABLE = "dc3_data.dc3_notify_channel_bind";

    private final DatabaseClient databaseClient;
    private final NotifyBuilder notifyBuilder;
    private final MessageBuilder messageBuilder;
    private final NotifyChannelBuilder notifyChannelBuilder;
    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    @Override
    public Mono<NotifyBO> getNotify(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        String sql = "SELECT id,notify_name,notify_code,auto_confirm_flag,notify_interval,notify_ext,enable_flag,"
                + "tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted"
                + " FROM " + NOTIFY_TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map((row, metadata) -> notifyBuilder.buildBOByDO(mapNotify(row)))
                .one();
    }

    @Override
    public Mono<MessageBO> getMessage(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        String sql = "SELECT id,message_name,message_code,message_level,message_ext,enable_flag,tenant_id,remark,"
                + "creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted"
                + " FROM " + MESSAGE_TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map((row, metadata) -> messageBuilder.buildBOByDO(mapMessage(row)))
                .one();
    }

    @Override
    public Mono<NotifyChannelBO> getChannel(long tenantId, long id) {
        if (!valid(tenantId) || !valid(id)) return Mono.empty();
        String sql = "SELECT id,channel_name,channel_code,channel_type_flag,credential_ref,channel_ext,enable_flag,"
                + "tenant_id,remark,creator_id,creator_name,create_time,operator_id,operator_name,operate_time,deleted"
                + " FROM " + CHANNEL_TABLE + " WHERE tenant_id=:tenant_id AND id=:id AND deleted=0 LIMIT 1";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("id", id)
                .map((row, metadata) -> notifyChannelBuilder.buildBOByDO(mapChannel(row)))
                .one();
    }

    @Override
    public Flux<NotifyChannelBindBO> listEnabledBinds(long tenantId, long notifyId) {
        if (!valid(tenantId) || !valid(notifyId)) return Flux.empty();
        String sql = "SELECT id,notify_id,channel_id,bind_ext,enable_flag,tenant_id,remark,creator_id,creator_name,"
                + "create_time,operator_id,operator_name,operate_time,deleted FROM " + BIND_TABLE
                + " WHERE tenant_id=:tenant_id AND notify_id=:notify_id AND enable_flag=0 AND deleted=0 ORDER BY id";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("notify_id", notifyId)
                .map((row, metadata) -> notifyChannelBindBuilder.buildBOByDO(mapBind(row)))
                .all();
    }

    private NotifyDO mapNotify(io.r2dbc.spi.Row row) {
        NotifyDO value = new NotifyDO();
        value.setId(number(row.get("id")));
        value.setNotifyName(text(row.get("notify_name")));
        value.setNotifyCode(text(row.get("notify_code")));
        value.setAutoConfirmFlag(byteValue(row.get("auto_confirm_flag")));
        value.setNotifyInterval(number(row.get("notify_interval")));
        value.setNotifyExt(json(row.get("notify_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        value.setTenantId(number(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(number(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(number(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
        return value;
    }

    private MessageDO mapMessage(io.r2dbc.spi.Row row) {
        MessageDO value = new MessageDO();
        value.setId(number(row.get("id")));
        value.setMessageName(text(row.get("message_name")));
        value.setMessageCode(text(row.get("message_code")));
        value.setMessageLevel(byteValue(row.get("message_level")));
        value.setMessageExt(json(row.get("message_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        value.setTenantId(number(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(number(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(number(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
        return value;
    }

    private NotifyChannelDO mapChannel(io.r2dbc.spi.Row row) {
        NotifyChannelDO value = new NotifyChannelDO();
        value.setId(number(row.get("id")));
        value.setChannelName(text(row.get("channel_name")));
        value.setChannelCode(text(row.get("channel_code")));
        value.setChannelTypeFlag(byteValue(row.get("channel_type_flag")));
        value.setCredentialRef(text(row.get("credential_ref")));
        value.setChannelExt(json(row.get("channel_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        value.setTenantId(number(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(number(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(number(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
        return value;
    }

    private NotifyChannelBindDO mapBind(io.r2dbc.spi.Row row) {
        NotifyChannelBindDO value = new NotifyChannelBindDO();
        value.setId(number(row.get("id")));
        value.setNotifyId(number(row.get("notify_id")));
        value.setChannelId(number(row.get("channel_id")));
        value.setBindExt(json(row.get("bind_ext", String.class)));
        value.setEnableFlag(byteValue(row.get("enable_flag")));
        value.setTenantId(number(row.get("tenant_id")));
        value.setRemark(text(row.get("remark")));
        value.setCreatorId(number(row.get("creator_id")));
        value.setCreatorName(text(row.get("creator_name")));
        value.setCreateTime(time(row.get("create_time")));
        value.setOperatorId(number(row.get("operator_id")));
        value.setOperatorName(text(row.get("operator_name")));
        value.setOperateTime(time(row.get("operate_time")));
        value.setDeleted(byteValue(row.get("deleted")));
        return value;
    }

    private JsonExt json(String value) {
        if (value == null) return null;
        return JsonUtil.parseObject(value, JsonExt.class);
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Byte byteValue(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return null;
    }

    private boolean valid(long value) {
        return value > 0;
    }
}
