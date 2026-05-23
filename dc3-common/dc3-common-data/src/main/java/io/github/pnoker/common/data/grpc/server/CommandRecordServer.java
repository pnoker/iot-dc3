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
import io.github.pnoker.api.center.data.CommandRecordApiGrpc;
import io.github.pnoker.api.center.data.GrpcCommandCallVO;
import io.github.pnoker.api.center.data.GrpcCommandRecordDTO;
import io.github.pnoker.api.center.data.GrpcCommandRecordQuery;
import io.github.pnoker.api.center.data.GrpcPageCommandRecordDTO;
import io.github.pnoker.api.center.data.GrpcRCommandRecordDTO;
import io.github.pnoker.api.center.data.GrpcRPageCommandRecordDTO;
import io.github.pnoker.api.center.data.GrpcRString;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.CommandRecordService;
import io.github.pnoker.common.data.entity.model.CommandRecordDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.entity.vo.CommandRecordQueryVO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server implementation for the CommandRecord service.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandRecordServer extends CommandRecordApiGrpc.CommandRecordApiImplBase {

    private final CommandRecordService commandRecordService;

    @Override
    public void call(GrpcCommandCallVO request, StreamObserver<GrpcRString> responseObserver) {
        try {
            CommandCallVO vo = new CommandCallVO();
            vo.setDeviceId(request.getDeviceId());
            vo.setCommandId(request.getCommandId());
            vo.setParamValues(request.getParamValuesMap());
            String recordId = commandRecordService.call(request.getTenantId(), vo);

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
            log.error("CommandRecordServer.call failed", e);
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
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcRCommandRecordDTO> responseObserver) {
        try {
            CommandRecordDO recordDO = commandRecordService.getByRecordId(request.getValue());
            GrpcRCommandRecordDTO.Builder response = GrpcRCommandRecordDTO.newBuilder();

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
            log.error("CommandRecordServer.getByRecordId failed", e);
            responseObserver.onNext(GrpcRCommandRecordDTO.newBuilder()
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
    public void list(GrpcCommandRecordQuery request, StreamObserver<GrpcRPageCommandRecordDTO> responseObserver) {
        try {
            CommandRecordQueryVO queryVO = new CommandRecordQueryVO();
            queryVO.setDeviceId(request.getDeviceId() != 0 ? request.getDeviceId() : null);
            queryVO.setCommandId(request.getCommandId() != 0 ? request.getCommandId() : null);
            queryVO.setStatus(Objects.isNull(request.getStatus()) || request.getStatus().isEmpty() ? null : request.getStatus());
            queryVO.setPage(Math.toIntExact(request.getPage().getCurrent()));
            queryVO.setSize(Math.toIntExact(request.getPage().getSize()));

            Page<CommandRecordDO> page = commandRecordService.list(request.getTenantId(), queryVO);

            GrpcPageCommandRecordDTO.Builder pageDataBuilder = GrpcPageCommandRecordDTO.newBuilder()
                    .setPage(GrpcPage.newBuilder()
                            .setCurrent(page.getCurrent())
                            .setSize(page.getSize())
                            .setTotal(page.getTotal())
                            .setPages(page.getPages())
                            .build());
            page.getRecords().forEach(record -> pageDataBuilder.addData(toGrpcDTO(record)));

            responseObserver.onNext(GrpcRPageCommandRecordDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(true)
                            .setCode(ResponseEnum.OK.getCode())
                            .setMessage(ResponseEnum.OK.getText())
                            .build())
                    .setData(pageDataBuilder.build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("CommandRecordServer.list failed", e);
            responseObserver.onNext(GrpcRPageCommandRecordDTO.newBuilder()
                    .setResult(GrpcR.newBuilder()
                            .setOk(false)
                            .setCode(ResponseEnum.FAILURE.getCode())
                            .setMessage(e.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private GrpcCommandRecordDTO toGrpcDTO(CommandRecordDO recordDO) {
        return GrpcCommandRecordDTO.newBuilder()
                .setId(Objects.nonNull(recordDO.getId()) ? recordDO.getId() : 0)
                .setRecordId(Objects.nonNull(recordDO.getRecordId()) ? recordDO.getRecordId() : "")
                .setTenantId(Objects.nonNull(recordDO.getTenantId()) ? recordDO.getTenantId() : 0)
                .setDeviceId(Objects.nonNull(recordDO.getDeviceId()) ? recordDO.getDeviceId() : 0)
                .setCommandId(Objects.nonNull(recordDO.getCommandId()) ? recordDO.getCommandId() : 0)
                .setCommandCode(Objects.nonNull(recordDO.getCommandCode()) ? recordDO.getCommandCode() : "")
                .setStatus(Objects.nonNull(recordDO.getStatus()) ? recordDO.getStatus() : "")
                .setErrorCode(Objects.nonNull(recordDO.getErrorCode()) ? recordDO.getErrorCode() : "")
                .setErrorMessage(Objects.nonNull(recordDO.getErrorMessage()) ? recordDO.getErrorMessage() : "")
                .setSource(Objects.nonNull(recordDO.getSource()) ? recordDO.getSource() : "")
                .setSourceUserId(Objects.nonNull(recordDO.getSourceUserId()) ? recordDO.getSourceUserId() : 0)
                .setSchemaVersion(Objects.nonNull(recordDO.getSchemaVersion()) ? recordDO.getSchemaVersion() : 0)
                .build();
    }

}
