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
package io.github.pnoker.common.data.service;

import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.query.NotifyChannelQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/**
 * Notification channel service.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface NotifyChannelService {
    /** Add one notify configuration channel. */
    Mono<NotifyChannelBO> add(NotifyChannelBO value);

    /** Delete the notify configuration channel, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id);

    /** Update one notify configuration channel and emit the updated row. */
    Mono<NotifyChannelBO> update(NotifyChannelBO value);

    /** Resolve the notify configuration channel by its id. */
    Mono<NotifyChannelBO> getById(Long tenantId, Long id);

    /** Page notify configuration channels matching the tenant-scoped filters. */
    Mono<OffsetPage<NotifyChannelBO>> list(Long tenantId, NotifyChannelQuery query);
}
