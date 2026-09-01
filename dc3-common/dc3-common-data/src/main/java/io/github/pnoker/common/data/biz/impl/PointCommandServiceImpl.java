package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.data.entity.builder.PointCommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.data.repository.ReactivePointCommandContext;
import io.github.pnoker.common.data.repository.ReactivePointCommandStore;
import io.github.pnoker.common.data.validator.PointCommandValidator;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Reactive point command application service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandServiceImpl implements PointCommandService, PointCommandHistoryService {

    private final ReactivePointCommandContext commandContext;
    private final ReactivePointCommandStore commandStore;
    private final ReactiveMessageSender messageSender;
    private final PointCommandHistoryBuilder historyBuilder;
    private final PointCommandValidator commandValidator;

    @Override
    public Mono<String> read(Long tenantId, PointCommandReadBO entityBO) {
        if (entityBO == null) return Mono.error(new ServiceException("Point command is required"));
        return reuseIfPresent(tenantId, entityBO.getCommandId(), entityBO.getDeviceId(), entityBO.getPointId(),
                        PointCommandTypeEnum.READ, null)
                .switchIfEmpty(Mono.defer(() -> scope(tenantId, entityBO.getDeviceId(), entityBO.getPointId(), false)
                        .flatMap(scope -> submit(tenantId, entityBO.getCommandId(), scope,
                                PointCommandTypeEnum.READ, null, entityBO.getSource()))));
    }

    @Override
    public Mono<String> write(Long tenantId, PointCommandWriteBO entityBO) {
        if (entityBO == null) return Mono.error(new ServiceException("Point command is required"));
        return Mono.defer(() -> {
            commandValidator.validateWriteValue(entityBO.getValue());
            return reuseIfPresent(tenantId, entityBO.getCommandId(), entityBO.getDeviceId(), entityBO.getPointId(),
                            PointCommandTypeEnum.WRITE, entityBO.getValue())
                    .switchIfEmpty(Mono.defer(() -> scope(tenantId, entityBO.getDeviceId(), entityBO.getPointId(), true)
                            .flatMap(scope -> submit(tenantId, entityBO.getCommandId(), scope,
                                    PointCommandTypeEnum.WRITE, entityBO.getValue(), entityBO.getSource()))));
        });
    }

    @Override
    public Mono<PointCommandHistoryVO> getByCommandId(Long tenantId, String commandId) {
        return commandStore.find(tenantId, commandId).map(historyBuilder::buildVOByDO);
    }

    @Override
    public Mono<OffsetPage<PointCommandHistoryVO>> list(Long tenantId, PointCommandHistoryQueryVO queryVO) {
        PointCommandHistoryQueryVO query = queryVO == null ? new PointCommandHistoryQueryVO() : queryVO;
        long offset = query.getOffset() == null ? 0L : query.getOffset();
        int limit = query.getLimit() == null ? 50 : query.getLimit();
        return commandStore.list(tenantId, parseId(query.getDeviceId(), "deviceId"),
                        parseId(query.getPointId(), "pointId"), query.getStatus(), query.getType(),
                        offset, limit, query.getSort())
                .map(page -> OffsetPage.of(page.items().stream().map(historyBuilder::buildVOByDO).toList(),
                        page.offset(), page.limit(), page.total()));
    }

    private Mono<String> submit(Long tenantId, String requestedId, Scope scope,
                                PointCommandTypeEnum type, String value, PointCommandSourceEnum source) {
        return existing(tenantId, requestedId, scope, type, value).switchIfEmpty(Mono.defer(() -> {
            String commandId = requestedId == null || requestedId.isBlank()
                    ? UuidV7.next().toString() : requestedId;
            Instant now = Instant.now();
            PointCommandHistoryDO command = new PointCommandHistoryDO();
            command.setCommandId(commandId);
            command.setTenantId(tenantId);
            command.setType(type);
            command.setDeviceId(scope.device().getId());
            command.setPointId(scope.point().getId());
            command.setRequestValue(value);
            command.setStatus(PointCommandStatusEnum.PENDING);
            command.setSource(source == null ? PointCommandSourceEnum.HTTP : source);
            command.setOccurTime(local(now));
            command.setExpireTime(local(now.plusSeconds(10)));
            command.setSchemaVersion((short) 1);
            return commandStore.insert(command)
                    .map(saved -> new PersistedCommand(saved, true))
                    .onErrorResume(error -> {
                        if (requestedId == null || requestedId.isBlank()) {
                            return Mono.error(error);
                        }
                        return existing(tenantId, requestedId, scope, type, value)
                                .map(id -> new PersistedCommand(commandWithId(id), false))
                                .switchIfEmpty(Mono.error(error));
                    })
                    .flatMap(persisted -> {
                        if (!persisted.fresh()) {
                            return Mono.just(persisted.command().getCommandId());
                        }
                        return publishAndMarkSent(scope, type, value, commandId, tenantId);
                    });
        }));
    }

    private Mono<String> publishAndMarkSent(Scope scope, PointCommandTypeEnum type, String value,
                                             String commandId, Long tenantId) {
        return publish(scope, type, value, commandId, tenantId)
                .then(commandStore.markSent(tenantId, commandId, Instant.now()))
                .flatMap(marked -> marked ? Mono.just(commandId)
                        : Mono.error(new ServiceException("Point command disappeared before dispatch")))
                .onErrorResume(publishError -> commandStore.markPublishFailed(tenantId, commandId,
                                "BROKER_PUBLISH_FAILED", publishError.getMessage(), Instant.now())
                        .onErrorResume(markError -> {
                            log.error("Failed to persist point command publish failure, commandId={}", commandId,
                                    markError);
                            return Mono.just(false);
                        })
                        .then(Mono.error(new ServiceException(
                                "Failed to route point command to active driver owner", publishError))));
    }

    private PointCommandHistoryDO commandWithId(String commandId) {
        PointCommandHistoryDO command = new PointCommandHistoryDO();
        command.setCommandId(commandId);
        return command;
    }

    private Mono<String> reuseIfPresent(Long tenantId, String commandId, Long deviceId, Long pointId,
                                        PointCommandTypeEnum type, String value) {
        if (commandId == null || commandId.isBlank()) return Mono.empty();
        return commandStore.find(tenantId, commandId).flatMap(existing -> {
            if (java.util.Objects.equals(existing.getDeviceId(), deviceId)
                    && java.util.Objects.equals(existing.getPointId(), pointId)
                    && existing.getType() == type
                    && java.util.Objects.equals(existing.getRequestValue(), value)) {
                return Mono.just(existing.getCommandId());
            }
            return Mono.error(new ServiceException("Idempotency key is already used for another command"));
        });
    }

    private Mono<String> existing(Long tenantId, String commandId, Scope scope,
                                  PointCommandTypeEnum type, String value) {
        if (commandId == null || commandId.isBlank()) return Mono.empty();
        return commandStore.find(tenantId, commandId).flatMap(existing -> {
            if (java.util.Objects.equals(existing.getDeviceId(), scope.device().getId())
                    && java.util.Objects.equals(existing.getPointId(), scope.point().getId())
                    && existing.getType() == type
                    && java.util.Objects.equals(existing.getRequestValue(), value)) {
                return Mono.just(existing.getCommandId());
            }
            return Mono.error(new ServiceException("Idempotency key is already used for another command"));
        });
    }

    private Mono<Scope> scope(Long tenantId, Long deviceId, Long pointId, boolean write) {
        if (tenantId == null || deviceId == null || pointId == null) {
            return Mono.error(new ServiceException("Tenant, device and point are required"));
        }
        return commandContext.device(tenantId, deviceId)
                .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .flatMap(device -> {
                    if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
                        return Mono.error(new ServiceException("Device is disabled"));
                    }
                    return commandContext.point(tenantId, pointId)
                            .switchIfEmpty(Mono.error(new NotFoundException("Point does not exist")))
                            .flatMap(point -> validatePoint(device, point, write)
                                    .then(commandContext.driverByDevice(tenantId, deviceId)
                                            .switchIfEmpty(Mono.error(new ServiceException("No driver registered for this device"))))
                                    .flatMap(driver -> commandContext.activeOwner(tenantId, deviceId)
                                            .filter(owner -> owner.driverId() != null
                                                    && owner.driverId().equals(driver.getId())
                                                    && owner.ownerNode() != null && !owner.ownerNode().isBlank()
                                                    && owner.fencingToken() != null && owner.fencingToken() > 0)
                                            .switchIfEmpty(Mono.error(new ServiceException(
                                                    "Device has no active driver owner")))
                                            .map(owner -> new Scope(device, point, driver, owner))));
                });
    }

    private Mono<Void> validatePoint(FacadeDeviceBO device, FacadePointBO point, boolean write) {
        if (EnableFlagEnum.DISABLE.equals(point.getEnableFlag())) {
            return Mono.error(new ServiceException("Point is disabled"));
        }
        if (device.getProfileId() == null || !device.getProfileId().equals(point.getProfileId())) {
            return Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH));
        }
        if (write && !RwTypeEnum.WRITE_ONLY.equals(point.getRwFlag())
                && !RwTypeEnum.READ_WRITE.equals(point.getRwFlag())) {
            return Mono.error(new ServiceException("Point is not writable"));
        }
        return Mono.empty();
    }

    private Mono<Void> publish(Scope scope, PointCommandTypeEnum type, String value,
                               String commandId, Long tenantId) {
        PointCommandDTO dto = type == PointCommandTypeEnum.READ
                ? PointCommandDTO.ofRead(commandId, tenantId, scope.owner().ownerNode(), scope.owner().fencingToken(),
                scope.device().getId(), scope.point().getId())
                : PointCommandDTO.ofWrite(commandId, tenantId, scope.owner().ownerNode(), scope.owner().fencingToken(),
                scope.device().getId(), scope.point().getId(), value);
        return messageSender.sendConfirmed(MqMessage.builder()
                .topic(MqTopic.POINT_COMMAND)
                .partitionKey(scope.driver().getServiceName() + "." + scope.owner().ownerNode())
                .payload(dto)
                .header(MqHeaders.CORRELATION_ID, commandId)
                .header(MqHeaders.TENANT_ID, String.valueOf(tenantId))
                .build());
    }

    private Long parseId(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new ServiceException(field + " must be a number", exception);
        }
    }

    private LocalDateTime local(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private record Scope(FacadeDeviceBO device, FacadePointBO point,
                         FacadeDriverBO driver, FacadeDeviceOwnerBO owner) {
    }

    private record PersistedCommand(PointCommandHistoryDO command, boolean fresh) {
    }
}
