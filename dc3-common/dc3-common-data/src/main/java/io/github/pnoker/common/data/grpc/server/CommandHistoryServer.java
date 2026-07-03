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
import io.github.pnoker.api.center.data.CommandHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcCommandCallVO;
import io.github.pnoker.api.center.data.GrpcCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcCommandHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPageCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcRCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcRPageCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcRString;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
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
 * gRPC server implementation for the CommandHistory service.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHistoryServer extends CommandHistoryApiGrpc.CommandHistoryApiImplBase {

    private final CommandHistoryService commandHistoryService;

    @Override
    public void callCommand(GrpcCommandCallVO request, StreamObserver<GrpcRString> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            CommandCallBO entityBO = new CommandCallBO();
            entityBO.setDeviceId(request.getDeviceId());
            entityBO.setCommandId(request.getCommandId());
            entityBO.setParamValues(request.getParamValuesMap());
            String recordId = commandHistoryService.call(request.getTenantId(), entityBO);

            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcRFactory.ok())
                    .setData(recordId)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("CommandHistoryServer.callCommand failed", e);
            responseObserver.onNext(GrpcRString.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcRCommandHistoryDTO> responseObserver) {
        try {
            // record_id is globally unique but dc3_command_history is not tenant-whitelisted;
            // the GrpcStringQuery carries no tenant_id, so bypass tenant filtering here.
            CommandHistoryVO record = TenantContextHolder.runIgnore(() -> commandHistoryService.getByRecordId(request.getValue()));
            GrpcRCommandHistoryDTO.Builder response = GrpcRCommandHistoryDTO.newBuilder();

            if (Objects.nonNull(record)) {
                response.setResult(GrpcRFactory.ok());
                response.setData(toGrpcDTO(record));
            } else {
                response.setResult(GrpcRFactory.notFound());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("CommandHistoryServer.getByRecordId failed", e);
            responseObserver.onNext(GrpcRCommandHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listByPage(GrpcCommandHistoryQuery request, StreamObserver<GrpcRPageCommandHistoryDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            CommandHistoryQueryVO queryVO = new CommandHistoryQueryVO();
            queryVO.setDeviceId(request.getDeviceId() != 0 ? request.getDeviceId() : null);
            queryVO.setCommandId(request.getCommandId() != 0 ? request.getCommandId() : null);
            queryVO.setStatus(request.getStatus() != 0
                    ? PointCommandStatusEnum.ofIndex((byte) request.getStatus()) : null);
            queryVO.setPage(GrpcBuilderUtil.buildPagesByGrpcPage(request.getPage()));

            Page<CommandHistoryVO> page = commandHistoryService.list(request.getTenantId(), queryVO);

            GrpcPageCommandHistoryDTO.Builder pageDataBuilder = GrpcPageCommandHistoryDTO.newBuilder()
                    .setPage(GrpcPage.newBuilder()
                            .setCurrent(page.getCurrent())
                            .setSize(page.getSize())
                            .setTotal(page.getTotal())
                            .setPages(page.getPages())
                            .build());
            page.getRecords().forEach(record -> pageDataBuilder.addData(toGrpcDTO(record)));

            responseObserver.onNext(GrpcRPageCommandHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.ok())
                    .setData(pageDataBuilder.build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("CommandHistoryServer.listByPage failed", e);
            responseObserver.onNext(GrpcRPageCommandHistoryDTO.newBuilder()
                    .setResult(GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private GrpcCommandHistoryDTO toGrpcDTO(CommandHistoryVO record) {
        return GrpcCommandHistoryDTO.newBuilder()
                .setId(Objects.nonNull(record.getId()) ? record.getId() : 0)
                .setRecordId(Objects.nonNull(record.getRecordId()) ? record.getRecordId() : "")
                .setTenantId(Objects.nonNull(record.getTenantId()) ? record.getTenantId() : 0)
                .setDeviceId(Objects.nonNull(record.getDeviceId()) ? record.getDeviceId() : 0)
                .setCommandId(Objects.nonNull(record.getCommandId()) ? record.getCommandId() : 0)
                .setCommandCode(Objects.nonNull(record.getCommandCode()) ? record.getCommandCode() : "")
                .putAllParamValues(toStringMap(record.getParamValues()))
                .putAllResultValues(toStringMap(record.getResultValues()))
                .setConfigSnapshot(Objects.nonNull(record.getConfigSnapshot()) ? record.getConfigSnapshot() : "")
                .setStatus(Objects.nonNull(record.getStatus()) ? record.getStatus().getIndex() : 0)
                .setErrorCode(Objects.nonNull(record.getErrorCode()) ? record.getErrorCode() : "")
                .setErrorMessage(Objects.nonNull(record.getErrorMessage()) ? record.getErrorMessage() : "")
                .setSource(Objects.nonNull(record.getSource()) ? record.getSource().getIndex() : 0)
                .setSourceUserId(Objects.nonNull(record.getSourceUserId()) ? record.getSourceUserId() : 0)
                .setOccurTime(toEpochSecond(record.getOccurTime()))
                .setSendTime(toEpochSecond(record.getSendTime()))
                .setFinishTime(toEpochSecond(record.getFinishTime()))
                .setSchemaVersion(Objects.nonNull(record.getSchemaVersion()) ? record.getSchemaVersion() : 0)
                .setCreateTime(toEpochSecond(record.getCreateTime()))
                .setOperateTime(toEpochSecond(record.getOperateTime()))
                .setExpireTime(toEpochSecond(record.getExpireTime()))
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
