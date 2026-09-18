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

import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

/** Reactive persistence port for notification configuration administration. */
public interface ReactiveNotifyAdminStore {
    /** Load the notify configuration scoped to the tenant by id. */
    Mono<NotifyDO> getNotify(long tenantId, long id);

    /** Page notify configurations matching the tenant-scoped filters. */
    Mono<OffsetPage<NotifyDO>> listNotify(
            long tenantId,
            String name,
            String code,
            AutoConfirmFlagEnum autoConfirm,
            Long interval,
            EnableFlagEnum enable,
            PageRequest page);

    /** Insert one notify configuration and emit the stored row. */
    Mono<NotifyDO> insertNotify(NotifyDO value);

    /** Update one notify configuration and emit the updated row. */
    Mono<NotifyDO> updateNotify(NotifyDO value);

    /** Delete the notify configuration, reporting whether a row was removed. */
    Mono<Boolean> deleteNotify(long tenantId, long id);

    /** Report whether the notify configuration has channel bindings. */
    Mono<Boolean> hasNotifyBindings(long tenantId, long notifyId);

    /** Check whether the notify configuration code exists under the tenant. */
    Mono<Boolean> existsNotifyCode(long tenantId, String code, Long excludedId);

    /** Load the message scoped to the tenant by id. */
    Mono<MessageDO> getMessage(long tenantId, long id);

    /** Page messages matching the tenant-scoped filters. */
    Mono<OffsetPage<MessageDO>> listMessage(
            long tenantId,
            String name,
            String code,
            AlarmMessageLevelEnum level,
            EnableFlagEnum enable,
            PageRequest page);

    /** Insert one message and emit the stored row. */
    Mono<MessageDO> insertMessage(MessageDO value);

    /** Update one message and emit the updated row. */
    Mono<MessageDO> updateMessage(MessageDO value);

    /** Delete the message, reporting whether a row was removed. */
    Mono<Boolean> deleteMessage(long tenantId, long id);

    /** Check whether the message code exists under the tenant. */
    Mono<Boolean> existsMessageCode(long tenantId, String code, Long excludedId);

    /** Load the channel scoped to the tenant by id. */
    Mono<NotifyChannelDO> getChannel(long tenantId, long id);

    /** Page channels matching the tenant-scoped filters. */
    Mono<OffsetPage<NotifyChannelDO>> listChannel(
            long tenantId,
            String name,
            String code,
            NotifyChannelTypeEnum type,
            EnableFlagEnum enable,
            PageRequest page);

    /** Insert one channel and emit the stored row. */
    Mono<NotifyChannelDO> insertChannel(NotifyChannelDO value);

    /** Update one channel and emit the updated row. */
    Mono<NotifyChannelDO> updateChannel(NotifyChannelDO value);

    /** Delete the channel, reporting whether a row was removed. */
    Mono<Boolean> deleteChannel(long tenantId, long id);

    /** Report whether the channel is referenced by a notify configuration. */
    Mono<Boolean> hasChannelBindings(long tenantId, long channelId);

    /** Check whether the channel code exists under the tenant. */
    Mono<Boolean> existsChannelCode(long tenantId, String code, Long excludedId);

    /** Load the binding scoped to the tenant by id. */
    Mono<NotifyChannelBindDO> getBind(long tenantId, long id);

    /** Page bindings matching the tenant-scoped filters. */
    Mono<OffsetPage<NotifyChannelBindDO>> listBind(
            long tenantId, Long notifyId, Long channelId, EnableFlagEnum enable, PageRequest page);

    /** Insert one binding and emit the stored row. */
    Mono<NotifyChannelBindDO> insertBind(NotifyChannelBindDO value);

    /** Update one binding and emit the updated row. */
    Mono<NotifyChannelBindDO> updateBind(NotifyChannelBindDO value);

    /** Delete the binding, reporting whether a row was removed. */
    Mono<Boolean> deleteBind(long tenantId, long id);

    /** Check whether the binding exists under the tenant. */
    Mono<Boolean> existsBind(long tenantId, long notifyId, long channelId, Long excludedId);

    /** Check whether the notify configuration exists under the tenant. */
    Mono<Boolean> existsNotify(long tenantId, long id);

    /** Check whether the channel exists under the tenant. */
    Mono<Boolean> existsChannel(long tenantId, long id);
}
