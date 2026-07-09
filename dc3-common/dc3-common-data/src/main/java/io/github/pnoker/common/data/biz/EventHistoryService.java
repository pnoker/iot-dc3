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

package io.github.pnoker.common.data.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.bo.EventReportBO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.entity.dto.EventReportDTO;

/**
 * Business service for event report operations.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public interface EventHistoryService {

    /**
     * Report an event over HTTP: validate the device/event scope, persist a history
     * record, and return the generated record id.
     *
     * @param tenantId tenant scope
     * @param entityBO {@link EventReportBO} event report payload
     * @return the generated record id
     */
    String report(Long tenantId, EventReportBO entityBO);

    /**
     * Report an event over the message bus: validate the device/event scope and persist
     * a history record using the record id carried in the DTO (along with its config
     * snapshot).
     *
     * @param entityDTO {@link EventReportDTO} event report payload from the bus
     * @return the record id from the DTO
     */
    String report(EventReportDTO entityDTO);

    /**
     * Get a single event history record by record id, scoped to a tenant.
     *
     * @param tenantId tenant scope; null queries across tenants
     * @param recordId the record id
     * @return the event history, or {@code null} if not found
     */
    EventHistoryVO getByRecordId(Long tenantId, String recordId);

    /**
     * Get a single event history record by record id across tenants.
     *
     * @param recordId the record id
     * @return the event history, or {@code null} if not found
     */
    default EventHistoryVO getByRecordId(String recordId) {
        return getByRecordId(null, recordId);
    }

    /**
     * Page event history records by query, scoped to a tenant.
     *
     * @param tenantId tenant scope
     * @param queryVO  filter and pagination parameters
     * @return the matching event history page
     */
    Page<EventHistoryVO> list(Long tenantId, EventHistoryQueryVO queryVO);

}
