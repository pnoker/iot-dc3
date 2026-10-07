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
package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.data.EventHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcEventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcEventHistoryOffsetPage;
import io.github.pnoker.api.center.data.GrpcEventHistoryQuery;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.common.enums.EventHistoryAcknowledgeFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.facade.api.EventHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * gRPC EventHistoryFacade: forwards event history reads to Data Center via
 * {@link EventHistoryApiGrpc}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventHistoryGrpcFacade implements EventHistoryFacade {

    private final EventHistoryApiGrpc.EventHistoryApiStub eventHistoryApiStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    private final ObjectMapper objectMapper;

    @Override
    public Mono<FacadeEventHistoryBO> getByRecordIdReactive(Long tenantId, String recordId) {
        if (Objects.isNull(tenantId) || tenantId <= 0 || Objects.isNull(recordId) || recordId.isBlank()) {
            return Mono.empty();
        }
        GrpcStringQuery request = GrpcStringQuery.newBuilder()
                .setTenantId(tenantId)
                .setValue(recordId)
                .build();
        return ReactiveGrpcClientSupport.<GrpcStringQuery, GrpcEventHistoryDTO>unary(
                        "EventHistoryFacade.getByRecordId",
                        observer ->
                                grpcFacadeSupport.withDeadline(eventHistoryApiStub).getByRecordId(request, observer))
                .map(this::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeEventHistoryBO>> listReactive(
            Long tenantId, String deviceId, String eventId, long offset, int limit) {
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            return Mono.empty();
        }
        GrpcEventHistoryQuery.Builder builder = GrpcEventHistoryQuery.newBuilder()
                .setTenantId(tenantId)
                .setPage(
                        io.github.pnoker.api.common.PageRequest.newBuilder()
                                .setOffset(Math.max(0, offset))
                                .setLimit(Math.max(1, limit)));
        if (Objects.nonNull(deviceId) && !deviceId.isBlank()) {
            builder.setDeviceId(Long.parseLong(deviceId.trim()));
        }
        if (Objects.nonNull(eventId) && !eventId.isBlank()) {
            builder.setEventId(Long.parseLong(eventId.trim()));
        }
        GrpcEventHistoryQuery request = builder.build();
        return ReactiveGrpcClientSupport.<GrpcEventHistoryQuery, GrpcEventHistoryOffsetPage>unary(
                        "EventHistoryFacade.list",
                        observer -> grpcFacadeSupport.withDeadline(eventHistoryApiStub).list(request, observer))
                .map(this::toFacadePage);
    }

    private OffsetPage<FacadeEventHistoryBO> toFacadePage(GrpcEventHistoryOffsetPage page) {
        return new OffsetPage<>(
                page.getItemsList().stream().map(this::toFacadeBO).toList(),
                page.getPage().getOffset(),
                page.getPage().getLimit(),
                page.getPage().getTotal(),
                page.getPage().getHasNext());
    }

    private FacadeEventHistoryBO toFacadeBO(GrpcEventHistoryDTO dto) {
        return new FacadeEventHistoryBO(
                text(dto.getRecordId()),
                dto.getDeviceId() > 0 ? String.valueOf(dto.getDeviceId()) : null,
                dto.getEventId() > 0 ? String.valueOf(dto.getEventId()) : null,
                text(dto.getEventCode()),
                dto.getEventTypeFlag() > 0 ? EventTypeFlagEnum.ofIndex((byte) dto.getEventTypeFlag()).getCode() : null,
                dto.getEventLevelFlag() > 0 ? EventLevelEnum.ofIndex((byte) dto.getEventLevelFlag()).getCode() : null,
                json(dto.getParamValuesMap()),
                text(dto.getMessage()),
                iso(dto.getOccurTime()),
                iso(dto.getReceiveTime()),
                dto.getAcknowledgeFlag() > 0
                        ? EventHistoryAcknowledgeFlagEnum.ofIndex((byte) dto.getAcknowledgeFlag()).getCode()
                        : null);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String iso(long epochSecond) {
        if (epochSecond <= 0) {
            return null;
        }
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
        return time.toString();
    }

    private String json(java.util.Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(new TreeMap<>(values));
    }
}
