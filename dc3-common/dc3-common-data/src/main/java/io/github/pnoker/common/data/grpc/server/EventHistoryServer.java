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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.EventHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcEventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcEventHistoryQuery;
import io.github.pnoker.api.center.data.GrpcEventReportVO;
import io.github.pnoker.api.center.data.GrpcPageEventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcREventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcRPageEventHistoryDTO;
import io.github.pnoker.api.center.data.GrpcRString;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.entity.bo.EventReportBO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/**
 * gRPC server implementation for the EventReport service.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventHistoryServer extends EventHistoryApiGrpc.EventHistoryApiImplBase {

    private final EventHistoryService eventHistoryService;

    @Override
    public void reportEvent(GrpcEventReportVO request, StreamObserver<GrpcRString> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            EventReportBO entityBO = new EventReportBO();
            entityBO.setDeviceId(request.getDeviceId());
            entityBO.setEventId(request.getEventId());
            entityBO.setParamValues(request.getParamValuesMap());
            entityBO.setMessage(request.getMessage());
            String recordId = eventHistoryService.report(request.getTenantId(), entityBO);

            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcRFactory.ok())
                    .setData(recordId)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.reportEvent failed", e);
            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcREventHistoryDTO> responseObserver) {
        try {
            // record_id is globally unique but dc3_event_history is not tenant-whitelisted;
            // the GrpcStringQuery carries no tenant_id, so bypass tenant filtering here.
            EventHistoryVO record = TenantContextHolder.runIgnore(() -> eventHistoryService.getByRecordId(request.getValue()));
            GrpcREventHistoryDTO.Builder response = GrpcREventHistoryDTO.newBuilder();

            if (Objects.nonNull(record)) {
                response.setResult(GrpcRFactory.ok());
                response.setData(toGrpcDTO(record));
            } else {
                response.setResult(GrpcRFactory.notFound());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.getByRecordId failed", e);
            responseObserver.onNext(GrpcREventHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listByPage(GrpcEventHistoryQuery request, StreamObserver<GrpcRPageEventHistoryDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            EventHistoryQueryVO queryVO = new EventHistoryQueryVO();
            queryVO.setDeviceId(request.getDeviceId() != 0 ? request.getDeviceId() : null);
            queryVO.setEventId(request.getEventId() != 0 ? request.getEventId() : null);
            if (request.getEventTypeFlag() != 0) {
                queryVO.setEventTypeFlag(EventTypeFlagEnum.ofIndex((byte) request.getEventTypeFlag()));
            }
            queryVO.setPage(GrpcBuilderUtil.buildPagesByGrpcPage(request.getPage()));

            Page<EventHistoryVO> page = eventHistoryService.list(request.getTenantId(), queryVO);

            GrpcPageEventHistoryDTO.Builder pageDataBuilder = GrpcPageEventHistoryDTO.newBuilder()
                    .setPage(GrpcPage.newBuilder()
                            .setCurrent(page.getCurrent())
                            .setSize(page.getSize())
                            .setTotal(page.getTotal())
                            .setPages(page.getPages())
                            .build());
            page.getRecords().forEach(record -> pageDataBuilder.addData(toGrpcDTO(record)));

            responseObserver.onNext(GrpcRPageEventHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.ok())
                    .setData(pageDataBuilder.build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.listByPage failed", e);
            responseObserver.onNext(GrpcRPageEventHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private GrpcEventHistoryDTO toGrpcDTO(EventHistoryVO record) {
        return GrpcEventHistoryDTO.newBuilder()
                .setId(Objects.nonNull(record.getId()) ? record.getId() : 0)
                .setRecordId(Objects.nonNull(record.getRecordId()) ? record.getRecordId() : "")
                .setTenantId(Objects.nonNull(record.getTenantId()) ? record.getTenantId() : 0)
                .setDeviceId(Objects.nonNull(record.getDeviceId()) ? record.getDeviceId() : 0)
                .setEventId(Objects.nonNull(record.getEventId()) ? record.getEventId() : 0)
                .setEventCode(Objects.nonNull(record.getEventCode()) ? record.getEventCode() : "")
                .setEventTypeFlag(Objects.nonNull(record.getEventTypeFlag()) ? record.getEventTypeFlag().getIndex() : 0)
                .setEventLevelFlag(Objects.nonNull(record.getEventLevelFlag()) ? record.getEventLevelFlag().getIndex() : 0)
                .putAllParamValues(toStringMap(record.getParamValues()))
                .setConfigSnapshot(Objects.nonNull(record.getConfigSnapshot()) ? record.getConfigSnapshot() : "")
                .setMessage(Objects.nonNull(record.getMessage()) ? record.getMessage() : "")
                .setOccurTime(toEpochSecond(record.getOccurTime()))
                .setReceiveTime(toEpochSecond(record.getReceiveTime()))
                .setAcknowledgeFlag(Objects.nonNull(record.getAcknowledgeFlag()) ? record.getAcknowledgeFlag().getIndex() : 0)
                .setSchemaVersion(Objects.nonNull(record.getSchemaVersion()) ? record.getSchemaVersion() : 0)
                .setCreateTime(toEpochSecond(record.getCreateTime()))
                .setOperateTime(toEpochSecond(record.getOperateTime()))
                .setAcknowledgeTime(toEpochSecond(record.getAcknowledgeTime()))
                .setAcknowledgeUserId(Objects.nonNull(record.getAcknowledgeUserId()) ? record.getAcknowledgeUserId() : 0)
                .build();
    }

    private long toEpochSecond(LocalDateTime value) {
        return Objects.nonNull(value) ? value.toEpochSecond(ZoneOffset.UTC) : 0;
    }

    private Map<String, String> toStringMap(String json) {
        if (Objects.isNull(json) || json.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {
        });
        return Objects.nonNull(values) ? values : Map.of();
    }

}
