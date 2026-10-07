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

import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral command history facade for reading custom command execution records.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface CommandHistoryFacade {

    /**
     * Look up one command history record by its record identifier.
     *
     * @param tenantId tenant id
     * @param recordId record identifier
     * @return command history mono, empty when the record does not exist
     */
    Mono<FacadeCommandHistoryBO> getByRecordIdReactive(Long tenantId, String recordId);

    /**
     * List command history records with canonical offset pagination.
     *
     * @param tenantId tenant id
     * @param deviceId optional device id filter
     * @param commandId optional command id filter
     * @param commandCode optional command code filter
     * @param status optional status code filter
     * @param offset canonical pagination offset
     * @param limit canonical pagination limit
     * @return command history page mono
     */
    Mono<OffsetPage<FacadeCommandHistoryBO>> listReactive(
            Long tenantId,
            String deviceId,
            String commandId,
            String commandCode,
            String status,
            long offset,
            int limit);
}
