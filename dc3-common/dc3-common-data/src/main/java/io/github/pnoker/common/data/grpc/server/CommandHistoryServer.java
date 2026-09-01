package io.github.pnoker.common.data.grpc.server;

import io.github.pnoker.api.center.data.CommandHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcCommandCallVO;
import io.github.pnoker.api.center.data.GrpcCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcCommandHistoryQuery;
import io.github.pnoker.api.center.data.GrpcCommandAccepted;
import io.github.pnoker.api.center.data.GrpcCommandHistoryOffsetPage;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.api.common.OffsetPage;
import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.grpc.GrpcPageUtil;
import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.utils.JsonUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Non-blocking gRPC server for custom command history. */
@Service
@RequiredArgsConstructor
public class CommandHistoryServer extends CommandHistoryApiGrpc.CommandHistoryApiImplBase {

    private final CommandHistoryService commandHistoryService;

    @Override
    public void callCommand(GrpcCommandCallVO request, StreamObserver<GrpcCommandAccepted> observer) {
        CommandCallBO command = new CommandCallBO();
        command.setDeviceId(request.getDeviceId());
        command.setCommandId(request.getCommandId() == 0 ? null : request.getCommandId());
        command.setCommandCode(request.getCommandCode().isBlank() ? null : request.getCommandCode());
        command.setParamValues(request.getParamValuesMap());
        command.setSource(CommandHistorySourceEnum.GRPC);
        ReactiveGrpcServerSupport.subscribe(commandHistoryService.call(request.getTenantId(), command)
                .map(recordId -> GrpcCommandAccepted.newBuilder().setRecordId(recordId).build()), observer);
    }

    @Override
    public void getByRecordId(GrpcStringQuery request, StreamObserver<GrpcCommandHistoryDTO> observer) {
        if (request.getTenantId() <= 0) {
            observer.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("tenant_id is required").asRuntimeException());
            return;
        }
        ReactiveGrpcServerSupport.subscribe(commandHistoryService.getByRecordId(request.getTenantId(), request.getValue())
                .switchIfEmpty(Mono.error(new NotFoundException("Command history does not exist")))
                .map(this::toGrpcDTO), observer);
    }

    @Override
    public void list(GrpcCommandHistoryQuery request,
                     StreamObserver<GrpcCommandHistoryOffsetPage> observer) {
        if (request.getTenantId() <= 0 || !request.hasPage()) {
            observer.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("tenant_id, offset or limit is invalid").asRuntimeException());
            return;
        }
        io.github.pnoker.db.r2dbc.core.page.PageRequest requestPage;
        try {
            requestPage = GrpcPageUtil.require(request.getPage());
        } catch (IllegalArgumentException error) {
            observer.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(error.getMessage()).asRuntimeException());
            return;
        }
        CommandHistoryQueryVO query = new CommandHistoryQueryVO();
        query.setDeviceId(request.hasDeviceId() ? String.valueOf(request.getDeviceId()) : null);
        query.setCommandId(request.hasCommandId() ? String.valueOf(request.getCommandId()) : null);
        query.setCommandCode(request.getCommandCode().isBlank() ? null : request.getCommandCode());
        query.setStatus(request.hasStatus() ? PointCommandStatusEnum.ofIndex((byte) request.getStatus()) : null);
        query.setOffset(requestPage.offset());
        query.setLimit(requestPage.limit());
        query.setSort(requestPage.sort());
        ReactiveGrpcServerSupport.subscribe(commandHistoryService.list(request.getTenantId(), query).map(page -> {
            GrpcCommandHistoryOffsetPage.Builder payload = GrpcCommandHistoryOffsetPage.newBuilder()
                    .setPage(OffsetPage.newBuilder().setOffset(page.offset()).setLimit(page.limit())
                            .setTotal(page.total()).setHasNext(page.hasNext()).build());
            page.items().forEach(item -> payload.addItems(toGrpcDTO(item)));
            return payload.build();
        }), observer);
    }

    private GrpcCommandHistoryDTO toGrpcDTO(CommandHistoryVO record) {
        return GrpcCommandHistoryDTO.newBuilder().setId(number(record.getId())).setRecordId(value(record.getRecordId()))
                .setTenantId(number(record.getTenantId())).setDeviceId(number(record.getDeviceId()))
                .setCommandId(number(record.getCommandId())).setCommandCode(value(record.getCommandCode()))
                .putAllParamValues(toStringMap(record.getParamValues())).putAllResultValues(toStringMap(record.getResultValues()))
                .setConfigSnapshot(value(record.getConfigSnapshot())).setStatus(record.getStatus() == null ? 0 : record.getStatus().getIndex())
                .setErrorCode(value(record.getErrorCode())).setErrorMessage(value(record.getErrorMessage()))
                .setSource(record.getSource() == null ? 0 : record.getSource().getIndex()).setSourceUserId(number(record.getSourceUserId()))
                .setOccurTime(epoch(record.getOccurTime())).setSendTime(epoch(record.getSendTime()))
                .setFinishTime(epoch(record.getFinishTime())).setSchemaVersion(record.getSchemaVersion() == null ? 0 : record.getSchemaVersion())
                .setCreateTime(epoch(record.getCreateTime())).setOperateTime(epoch(record.getOperateTime()))
                .setExpireTime(epoch(record.getExpireTime())).build();
    }

    private long number(String value) {
        if (value == null || value.isBlank()) return 0L;
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return 0L; }
    }
    private String value(String value) { return value == null ? "" : value; }
    private long epoch(LocalDateTime value) { return value == null ? 0L : value.toEpochSecond(ZoneOffset.UTC); }
    private Map<String, String> toStringMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        Map<String, String> values = JsonUtil.parseObject(json, new TypeReference<Map<String, String>>() {});
        return values == null ? Map.of() : values;
    }
}
