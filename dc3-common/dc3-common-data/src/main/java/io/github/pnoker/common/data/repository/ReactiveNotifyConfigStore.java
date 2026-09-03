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

import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped reads for notification configuration used by alarm delivery. */
public interface ReactiveNotifyConfigStore {

    /** Load the notify configuration scoped to the tenant by id. */
    Mono<NotifyBO> getNotify(long tenantId, long id);

    /** Load the message scoped to the tenant by id. */
    Mono<MessageBO> getMessage(long tenantId, long id);

    /** Load the channel scoped to the tenant by id. */
    Mono<NotifyChannelBO> getChannel(long tenantId, long id);

    /** Stream the enabled notify bindings for the tenant. */
    Flux<NotifyChannelBindBO> listEnabledBinds(long tenantId, long notifyId);
}
