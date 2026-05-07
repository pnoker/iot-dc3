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

import io.github.pnoker.api.center.data.*;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * gRPC server implementation for the PointValue service. Delegates to
 * {@link PointValueService} and {@link PointValueCommandService}.
 */
@Slf4j
@Service
public class PointValueServer extends PointValueApiGrpc.PointValueApiImplBase {

    private final PointValueService pointValueService;

    private final PointValueCommandService pointValueCommandService;

    public PointValueServer(PointValueService pointValueService, PointValueCommandService pointValueCommandService) {
        this.pointValueService = pointValueService;
        this.pointValueCommandService = pointValueCommandService;
    }

    @Override
    public void lastValue(GrpcPointValueQuery request, StreamObserver<GrpcRPointValueDTO> responseObserver) {
        try {
            // latest() with a page query — simplified: query by device+point, return
            // first result
            io.github.pnoker.common.entity.query.PointValueQuery query = new io.github.pnoker.common.entity.query.PointValueQuery();
            query.setDeviceId(request.getDeviceId());
            query.setPointId(request.getPointId());
            query.setTenantId(request.getTenantId());
            io.github.pnoker.common.entity.common.Pages pages = new io.github.pnoker.common.entity.common.Pages();
            pages.setCurrent(1);
            pages.setSize(1);
            query.setPage(pages);

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<io.github.pnoker.common.entity.bo.PointValueBO> page = pointValueService
                    .latest(query);

            GrpcRPointValueDTO.Builder response = GrpcRPointValueDTO.newBuilder();
            response.setResult(GrpcR.newBuilder()
                    .setOk(true)
                    .setCode(ResponseEnum.OK.getCode())
                    .setMessage(ResponseEnum.OK.getText())
                    .build());

            if (page != null && !page.getRecords().isEmpty()) {
                io.github.pnoker.common.entity.bo.PointValueBO bo = page.getRecords().getFirst();
                response.setData(GrpcPointValueDTO.newBuilder()
                        .setId(0)
                        .setDeviceId(bo.getDeviceId() != null ? bo.getDeviceId() : 0)
                        .setPointId(bo.getPointId() != null ? bo.getPointId() : 0)
                        .setValue(bo.getCalValue() != null ? bo.getCalValue() : "")
                        .setRawValue(bo.getRawValue() != null ? bo.getRawValue() : "")
                        .setCreateTime(
                                bo.getCreateTime() != null ? bo.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) : 0)
                        .build());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.lastValue error: {}", e.getMessage(), e);
            responseObserver.onNext(GrpcRPointValueDTO.newBuilder()
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
    public void historyValue(GrpcPointValueHistoryQuery request,
                             StreamObserver<GrpcRPointValueStringList> responseObserver) {
        try {
            List<String> history = pointValueService.history(request.getTenantId(), request.getDeviceId(),
                    request.getPointId(), request.getCount());

            GrpcRPointValueStringList.Builder response = GrpcRPointValueStringList.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build());

            if (history != null) {
                response.addAllData(history);
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.historyValue error: {}", e.getMessage(), e);
            responseObserver.onNext(GrpcRPointValueStringList.newBuilder()
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
    public void readCommand(GrpcPointValueCommandQuery request, StreamObserver<GrpcRBoolean> responseObserver) {
        try {
            PointValueReadVO vo = new PointValueReadVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setPointId(request.getPointId());
            pointValueCommandService.read(vo);

            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build())
                    .setData(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.readCommand error: {}", e.getMessage(), e);
            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .setData(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void writeCommand(GrpcPointValueWriteCommand request, StreamObserver<GrpcRBoolean> responseObserver) {
        try {
            PointValueWriteVO vo = new PointValueWriteVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setPointId(request.getPointId());
            vo.setValue(request.getValue());
            pointValueCommandService.write(vo);

            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build())
                    .setData(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.writeCommand error: {}", e.getMessage(), e);
            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .setData(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

}
