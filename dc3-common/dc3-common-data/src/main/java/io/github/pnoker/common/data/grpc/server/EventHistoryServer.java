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
package io.github.pnoker.common.data.grpc.server;

import io.github.pnoker.api.center.data.EventHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcEventAccepted;
import io.github.pnoker.api.center.data.GrpcEventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcEventHistoryOffsetPage;
import io.github.pnoker.api.center.data.GrpcEventHistoryQuery;
import io.github.pnoker.api.center.data.GrpcEventReportVO;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.grpc.GrpcPageUtil;
import io.github.pnoker.common.data.entity.bo.EventReportBO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import io.github.pnoker.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** gRPC server implementation for event history. */
@Service
@RequiredArgsConstructor
public class EventHistoryServer extends EventHistoryApiGrpc.EventHistoryApiImplBase {

    private final EventHistoryService eventHistoryService;

    @Override
    public void reportEvent(GrpcEventReportVO request, StreamObserver<GrpcEventAccepted> responseObserver) {
        EventReportBO report = new EventReportBO();
        report.setDeviceId(request.getDeviceId());
        report.setEventId(request.getEventId());
        report.setParamValues(request.getParamValuesMap());
        report.setMessage(request.getMessage());
        ReactiveGrpcServerSupport.subscribe(eventHistoryService.report(request.getTenantId(), report)
                .map(id -> GrpcEventAccepted.newBuilder().setRecordId(id).build()), responseObserver);
    }

    @Override
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcEventHistoryDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(eventHistoryService.getByRecordId(request.getTenantId(), request.getValue())
                .switchIfEmpty(Mono.error(new io.github.pnoker.common.exception.NotFoundException("event history record does not exist")))
                .map(this::toGrpcDTO), responseObserver);
    }

    @Override
    public void list(GrpcEventHistoryQuery request, StreamObserver<GrpcEventHistoryOffsetPage> responseObserver) {
        if (request.getTenantId() <= 0 || !request.hasPage()) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("tenant_id, offset or limit is invalid").asRuntimeException());
            return;
        }
        io.github.pnoker.db.r2dbc.core.page.PageRequest page;
        try {
            page = GrpcPageUtil.require(request.getPage());
        } catch (IllegalArgumentException error) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(error.getMessage()).asRuntimeException());
            return;
        }
        EventHistoryQueryVO query = new EventHistoryQueryVO();
        query.setDeviceId(request.hasDeviceId() ? String.valueOf(request.getDeviceId()) : null);
        query.setEventId(request.hasEventId() ? String.valueOf(request.getEventId()) : null);
        if (request.hasEventTypeFlag()) query.setEventTypeFlag(EventTypeFlagEnum.ofIndex((byte) request.getEventTypeFlag()));
        query.setOffset(page.offset());
        query.setLimit(page.limit());
        query.setSort(page.sort());
        ReactiveGrpcServerSupport.subscribe(eventHistoryService.list(request.getTenantId(), query).map(this::toGrpcPage), responseObserver);
    }

    private GrpcEventHistoryOffsetPage toGrpcPage(OffsetPage<EventHistoryVO> page) {
        GrpcEventHistoryOffsetPage.Builder builder = GrpcEventHistoryOffsetPage.newBuilder()
                .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder().setOffset(page.offset()).setLimit(page.limit())
                        .setTotal(page.total()).setHasNext(page.hasNext()).build());
        page.items().forEach(record -> builder.addItems(toGrpcDTO(record)));
        return builder.build();
    }

    private GrpcEventHistoryDTO toGrpcDTO(EventHistoryVO record) {
        return GrpcEventHistoryDTO.newBuilder()
                .setId(parse(record.getId())).setRecordId(text(record.getRecordId())).setTenantId(parse(record.getTenantId()))
                .setDeviceId(parse(record.getDeviceId())).setEventId(parse(record.getEventId())).setEventCode(text(record.getEventCode()))
                .setEventTypeFlag(record.getEventTypeFlag() == null ? 0 : record.getEventTypeFlag().getIndex())
                .setEventLevelFlag(record.getEventLevelFlag() == null ? 0 : record.getEventLevelFlag().getIndex())
                .putAllParamValues(toStringMap(record.getParamValues())).setConfigSnapshot(text(record.getConfigSnapshot()))
                .setMessage(text(record.getMessage())).setOccurTime(toEpochSecond(record.getOccurTime())).setReceiveTime(toEpochSecond(record.getReceiveTime()))
                .setAcknowledgeFlag(record.getAcknowledgeFlag() == null ? 0 : record.getAcknowledgeFlag().getIndex())
                .setSchemaVersion(record.getSchemaVersion() == null ? 0 : record.getSchemaVersion()).setCreateTime(toEpochSecond(record.getCreateTime()))
                .setOperateTime(toEpochSecond(record.getOperateTime())).setAcknowledgeTime(toEpochSecond(record.getAcknowledgeTime())).setAcknowledgeUserId(parse(record.getAcknowledgeUserId())).build();
    }

    private long parse(String value) { if (value == null || value.isBlank()) return 0; try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return 0; } }
    private String text(String value) { return value == null ? "" : value; }
    private long toEpochSecond(LocalDateTime value) { return value == null ? 0 : value.toEpochSecond(ZoneOffset.UTC); }
    private Map<String, String> toStringMap(String json) { if (json == null || json.isBlank()) return Map.of(); return Objects.requireNonNullElse(JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {}), Map.of()); }
}
