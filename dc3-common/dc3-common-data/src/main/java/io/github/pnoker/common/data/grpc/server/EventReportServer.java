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
import io.github.pnoker.api.center.data.EventReportApiGrpc;
import io.github.pnoker.api.center.data.GrpcEventRecordDTO;
import io.github.pnoker.api.center.data.GrpcEventRecordQuery;
import io.github.pnoker.api.center.data.GrpcEventReportVO;
import io.github.pnoker.api.center.data.GrpcPageEventRecordDTO;
import io.github.pnoker.api.center.data.GrpcREventRecordDTO;
import io.github.pnoker.api.center.data.GrpcRPageEventRecordDTO;
import io.github.pnoker.api.center.data.GrpcRString;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.EventReportService;
import io.github.pnoker.common.data.entity.model.EventRecordDO;
import io.github.pnoker.common.data.entity.vo.EventRecordQueryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class EventReportServer extends EventReportApiGrpc.EventReportApiImplBase {

    private final EventReportService eventReportService;

    @Override
    public void report(GrpcEventReportVO request, StreamObserver<GrpcRString> responseObserver) {
        try {
            EventReportVO vo = new EventReportVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setEventId(request.getEventId());
            vo.setParamValues(request.getParamValuesMap());
            vo.setMessage(request.getMessage());
            String recordId = eventReportService.report(request.getTenantId(), vo);

            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build())
                    .setData(recordId)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventReportServer.report failed", e);
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
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcREventRecordDTO> responseObserver) {
        try {
            EventRecordDO recordDO = eventReportService.getByRecordId(request.getValue());
            GrpcREventRecordDTO.Builder response = GrpcREventRecordDTO.newBuilder();

            if (Objects.nonNull(recordDO)) {
                response.setResult(GrpcR.newBuilder()
                        .setOk(true)
                        .setCode(ResponseEnum.OK.getCode())
                        .setMessage(ResponseEnum.OK.getText())
                        .build());
                response.setData(toGrpcDTO(recordDO));
            } else {
                response.setResult(GrpcR.newBuilder()
                        .setOk(false)
                        .setCode(ResponseEnum.NO_RESOURCE.getCode())
                        .setMessage(ResponseEnum.NO_RESOURCE.getText())
                        .build());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventReportServer.getByRecordId failed", e);
            responseObserver.onNext(GrpcREventRecordDTO.newBuilder()
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
    public void list(GrpcEventRecordQuery request, StreamObserver<GrpcRPageEventRecordDTO> responseObserver) {
        try {
            EventRecordQueryVO queryVO = new EventRecordQueryVO();
            queryVO.setDeviceId(request.getDeviceId() != 0 ? request.getDeviceId() : null);
            queryVO.setEventId(request.getEventId() != 0 ? request.getEventId() : null);
            if (request.getEventTypeFlag() != 0) {
                queryVO.setEventTypeFlag((byte) request.getEventTypeFlag());
            }
            queryVO.setPage(Math.toIntExact(request.getPage().getCurrent()));
            queryVO.setSize(Math.toIntExact(request.getPage().getSize()));

            Page<EventRecordDO> page = eventReportService.list(request.getTenantId(), queryVO);

            GrpcPageEventRecordDTO.Builder pageDataBuilder = GrpcPageEventRecordDTO.newBuilder()
                    .setPage(GrpcPage.newBuilder()
                            .setCurrent(page.getCurrent())
                            .setSize(page.getSize())
                            .setTotal(page.getTotal())
                            .setPages(page.getPages())
                            .build());
            page.getRecords().forEach(record -> pageDataBuilder.addData(toGrpcDTO(record)));

            responseObserver.onNext(GrpcRPageEventRecordDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build())
                    .setData(pageDataBuilder.build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("EventReportServer.list failed", e);
            responseObserver.onNext(GrpcRPageEventRecordDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private GrpcEventRecordDTO toGrpcDTO(EventRecordDO recordDO) {
        return GrpcEventRecordDTO.newBuilder()
                .setId(Objects.nonNull(recordDO.getId()) ? recordDO.getId() : 0)
                .setRecordId(Objects.nonNull(recordDO.getRecordId()) ? recordDO.getRecordId() : "")
                .setTenantId(Objects.nonNull(recordDO.getTenantId()) ? recordDO.getTenantId() : 0)
                .setDeviceId(Objects.nonNull(recordDO.getDeviceId()) ? recordDO.getDeviceId() : 0)
                .setEventId(Objects.nonNull(recordDO.getEventId()) ? recordDO.getEventId() : 0)
                .setEventCode(Objects.nonNull(recordDO.getEventCode()) ? recordDO.getEventCode() : "")
                .setEventTypeFlag(Objects.nonNull(recordDO.getEventTypeFlag()) ? recordDO.getEventTypeFlag() : 0)
                .setEventLevelFlag(Objects.nonNull(recordDO.getEventLevelFlag()) ? recordDO.getEventLevelFlag() : 0)
                .setMessage(Objects.nonNull(recordDO.getMessage()) ? recordDO.getMessage() : "")
                .setAcknowledgeFlag(Objects.nonNull(recordDO.getAcknowledgeFlag()) ? recordDO.getAcknowledgeFlag() : 0)
                .setSchemaVersion(Objects.nonNull(recordDO.getSchemaVersion()) ? recordDO.getSchemaVersion() : 0)
                .build();
    }

}
