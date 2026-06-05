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
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.enums.ResponseEnum;
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
        try {
            EventReportVO vo = new EventReportVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setEventId(request.getEventId());
            vo.setParamValues(request.getParamValuesMap());
            vo.setMessage(request.getMessage());
            String recordId = eventHistoryService.report(request.getTenantId(), vo);

            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getRemark())
                            .build())
                    .setData(recordId)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.reportEvent failed", e);
            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcREventHistoryDTO> responseObserver) {
        try {
            EventHistoryDO recordDO = eventHistoryService.getByRecordId(request.getValue());
            GrpcREventHistoryDTO.Builder response = GrpcREventHistoryDTO.newBuilder();

            if (Objects.nonNull(recordDO)) {
                response.setResult(GrpcR.newBuilder()
                        .setOk(true)
                        .setCode(ResponseEnum.OK.getCode())
                        .setMessage(ResponseEnum.OK.getRemark())
                        .build());
                response.setData(toGrpcDTO(recordDO));
            } else {
                response.setResult(GrpcR.newBuilder()
                        .setOk(false)
                        .setCode(ResponseEnum.NO_RESOURCE.getCode())
                        .setMessage(ResponseEnum.NO_RESOURCE.getRemark())
                        .build());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.getByRecordId failed", e);
            responseObserver.onNext(GrpcREventHistoryDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listByPage(GrpcEventHistoryQuery request, StreamObserver<GrpcRPageEventHistoryDTO> responseObserver) {
        try {
            EventHistoryQueryVO queryVO = new EventHistoryQueryVO();
            queryVO.setDeviceId(request.getDeviceId() != 0 ? request.getDeviceId() : null);
            queryVO.setEventId(request.getEventId() != 0 ? request.getEventId() : null);
            if (request.getEventTypeFlag() != 0) {
                queryVO.setEventTypeFlag((byte) request.getEventTypeFlag());
            }
            queryVO.setPage(GrpcBuilderUtil.buildPagesByGrpcPage(request.getPage()));

            Page<EventHistoryDO> page = eventHistoryService.list(request.getTenantId(), queryVO);

            GrpcPageEventHistoryDTO.Builder pageDataBuilder = GrpcPageEventHistoryDTO.newBuilder()
                    .setPage(GrpcPage.newBuilder()
                            .setCurrent(page.getCurrent())
                            .setSize(page.getSize())
                            .setTotal(page.getTotal())
                            .setPages(page.getPages())
                            .build());
            page.getRecords().forEach(record -> pageDataBuilder.addData(toGrpcDTO(record)));

            responseObserver.onNext(GrpcRPageEventHistoryDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getRemark())
                            .build())
                    .setData(pageDataBuilder.build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventHistoryServer.listByPage failed", e);
            responseObserver.onNext(GrpcRPageEventHistoryDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private GrpcEventHistoryDTO toGrpcDTO(EventHistoryDO recordDO) {
        return GrpcEventHistoryDTO.newBuilder()
                .setId(Objects.nonNull(recordDO.getId()) ? recordDO.getId() : 0)
                .setRecordId(Objects.nonNull(recordDO.getRecordId()) ? recordDO.getRecordId() : "")
                .setTenantId(Objects.nonNull(recordDO.getTenantId()) ? recordDO.getTenantId() : 0)
                .setDeviceId(Objects.nonNull(recordDO.getDeviceId()) ? recordDO.getDeviceId() : 0)
                .setEventId(Objects.nonNull(recordDO.getEventId()) ? recordDO.getEventId() : 0)
                .setEventCode(Objects.nonNull(recordDO.getEventCode()) ? recordDO.getEventCode() : "")
                .setEventTypeFlag(Objects.nonNull(recordDO.getEventTypeFlag()) ? recordDO.getEventTypeFlag() : 0)
                .setEventLevelFlag(Objects.nonNull(recordDO.getEventLevelFlag()) ? recordDO.getEventLevelFlag() : 0)
                .putAllParamValues(toStringMap(recordDO.getParamValues()))
                .setConfigSnapshot(Objects.nonNull(recordDO.getConfigSnapshot()) ? recordDO.getConfigSnapshot() : "")
                .setMessage(Objects.nonNull(recordDO.getMessage()) ? recordDO.getMessage() : "")
                .setOccurTime(toEpochSecond(recordDO.getOccurTime()))
                .setReceiveTime(toEpochSecond(recordDO.getReceiveTime()))
                .setAcknowledgeFlag(Objects.nonNull(recordDO.getAcknowledgeFlag()) ? recordDO.getAcknowledgeFlag() : 0)
                .setSchemaVersion(Objects.nonNull(recordDO.getSchemaVersion()) ? recordDO.getSchemaVersion() : 0)
                .setCreateTime(toEpochSecond(recordDO.getCreateTime()))
                .setOperateTime(toEpochSecond(recordDO.getOperateTime()))
                .setAcknowledgeTime(toEpochSecond(recordDO.getAcknowledgeTime()))
                .setAcknowledgeUserId(Objects.nonNull(recordDO.getAcknowledgeUserId()) ? recordDO.getAcknowledgeUserId() : 0)
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
