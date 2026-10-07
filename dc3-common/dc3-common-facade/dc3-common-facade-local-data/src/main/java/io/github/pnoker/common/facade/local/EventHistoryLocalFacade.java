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
package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.facade.api.EventHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process EventHistoryFacade backed by {@link EventHistoryService}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventHistoryLocalFacade implements EventHistoryFacade {

    private final EventHistoryService eventHistoryService;

    @Override
    public Mono<FacadeEventHistoryBO> getByRecordIdReactive(Long tenantId, String recordId) {
        return eventHistoryService.getByRecordId(tenantId, recordId).map(this::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeEventHistoryBO>> listReactive(
            Long tenantId, String deviceId, String eventId, long offset, int limit) {
        EventHistoryQueryVO query = new EventHistoryQueryVO();
        query.setDeviceId(blankToNull(deviceId));
        query.setEventId(blankToNull(eventId));
        query.setOffset(Math.max(0, offset));
        query.setLimit(Math.max(1, limit));
        return eventHistoryService
                .list(tenantId, query)
                .map(page -> new OffsetPage<>(
                        page.items().stream().map(this::toFacadeBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total(),
                        page.hasNext()));
    }

    private FacadeEventHistoryBO toFacadeBO(EventHistoryVO record) {
        return new FacadeEventHistoryBO(
                record.getRecordId(),
                record.getDeviceId(),
                record.getEventId(),
                record.getEventCode(),
                Objects.isNull(record.getEventTypeFlag())
                        ? null
                        : record.getEventTypeFlag().getCode(),
                Objects.isNull(record.getEventLevelFlag())
                        ? null
                        : record.getEventLevelFlag().getCode(),
                record.getParamValues(),
                record.getMessage(),
                iso(record.getOccurTime()),
                iso(record.getReceiveTime()),
                Objects.isNull(record.getAcknowledgeFlag())
                        ? null
                        : record.getAcknowledgeFlag().getCode());
    }

    private static String blankToNull(String value) {
        return Objects.isNull(value) || value.isBlank() ? null : value;
    }

    private static String iso(java.time.LocalDateTime value) {
        return Objects.isNull(value) ? null : value.toString();
    }
}
