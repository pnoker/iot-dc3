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

import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueDTO;
import io.github.pnoker.api.center.data.GrpcPointValueHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPointValueQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.GrpcRBoolean;
import io.github.pnoker.api.center.data.GrpcRPointValueDTO;
import io.github.pnoker.api.center.data.GrpcRPointValueStringList;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * gRPC server implementation for the PointValue service. Delegates to
 * {@link PointValueService} and {@link PointCommandService}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointValueServer extends PointValueApiGrpc.PointValueApiImplBase {

    private final PointValueService pointValueService;

    private final PointCommandService pointCommandService;

    @Override
    public void getLastValue(GrpcPointValueQuery request, StreamObserver<GrpcRPointValueDTO> responseObserver) {
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
            if (Objects.isNull(page) || page.getRecords().isEmpty()) {
                response.setResult(GrpcR.newBuilder()
                        .setOk(false)
                        .setCode(ResponseEnum.NO_RESOURCE.getCode())
                        .setMessage(ResponseEnum.NO_RESOURCE.getRemark())
                        .build());
            } else {
                response.setResult(GrpcR.newBuilder()
                        .setOk(true)
                        .setCode(ResponseEnum.OK.getCode())
                        .setMessage(ResponseEnum.OK.getRemark())
                        .build());

                io.github.pnoker.common.entity.bo.PointValueBO bo = page.getRecords().getFirst();
                response.setData(GrpcPointValueDTO.newBuilder()
                        .setId(0)
                        .setDeviceId(Objects.nonNull(bo.getDeviceId()) ? bo.getDeviceId() : 0)
                        .setPointId(Objects.nonNull(bo.getPointId()) ? bo.getPointId() : 0)
                        .setValue(Objects.nonNull(bo.getCalValue()) ? bo.getCalValue() : "")
                        .setRawValue(Objects.nonNull(bo.getRawValue()) ? bo.getRawValue() : "")
                        .setNumValue(Objects.nonNull(bo.getNumValue()) ? bo.getNumValue() : 0d)
                        .setCreateTime(
                                Objects.nonNull(bo.getCreateTime()) ? bo.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) : 0)
                        .build());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.getLastValue failed, tenantId={}, deviceId={}, pointId={}", request.getTenantId(),
                    request.getDeviceId(), request.getPointId(), e);
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
    public void listHistoryValues(GrpcPointValueHistoryQuery request,
                                  StreamObserver<GrpcRPointValueStringList> responseObserver) {
        try {
            List<String> history = pointValueService.history(request.getTenantId(), request.getDeviceId(),
                    request.getPointId(), request.getCount());

            GrpcRPointValueStringList.Builder response = GrpcRPointValueStringList.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getRemark())
                            .build());

            if (Objects.nonNull(history)) {
                response.addAllData(history);
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.listHistoryValues failed, tenantId={}, deviceId={}, pointId={}, count={}",
                    request.getTenantId(), request.getDeviceId(), request.getPointId(), request.getCount(), e);
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
            PointCommandReadVO vo = new PointCommandReadVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setPointId(request.getPointId());
            pointCommandService.read(request.getTenantId(), vo);

            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getRemark())
                            .build())
                    .setData(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.readCommand failed, tenantId={}, deviceId={}, pointId={}",
                    request.getTenantId(), request.getDeviceId(), request.getPointId(), e);
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
            PointCommandWriteVO vo = new PointCommandWriteVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setPointId(request.getPointId());
            vo.setValue(request.getValue());
            pointCommandService.write(request.getTenantId(), vo);

            responseObserver.onNext(GrpcRBoolean.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getRemark())
                            .build())
                    .setData(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PointValueServer.writeCommand failed, tenantId={}, deviceId={}, pointId={}",
                    request.getTenantId(), request.getDeviceId(), request.getPointId(), e);
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
