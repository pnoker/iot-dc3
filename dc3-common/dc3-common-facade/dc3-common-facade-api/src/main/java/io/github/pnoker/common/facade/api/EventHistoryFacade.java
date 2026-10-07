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
package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadeEventHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral event history facade for reading reported event records.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface EventHistoryFacade {

    /**
     * Look up one event history record by its record identifier.
     *
     * @param tenantId tenant id
     * @param recordId record identifier
     * @return event history mono, empty when the record does not exist
     */
    Mono<FacadeEventHistoryBO> getByRecordIdReactive(Long tenantId, String recordId);

    /**
     * List event history records with canonical offset pagination.
     *
     * @param tenantId tenant id
     * @param deviceId optional device id filter
     * @param eventId optional event id filter
     * @param offset canonical pagination offset
     * @param limit canonical pagination limit
     * @return event history page mono
     */
    Mono<OffsetPage<FacadeEventHistoryBO>> listReactive(
            Long tenantId, String deviceId, String eventId, long offset, int limit);
}
