package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.builder.CommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.data.repository.ReactiveCommandHistoryStore;
import io.github.pnoker.common.data.repository.ReactivePointCommandContext;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandOffsetQuery;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Reactive application service for custom command calls and history. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHistoryServiceImpl implements CommandHistoryService {

    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 30;

    private final DeviceFacade deviceFacade;
    private final DriverFacade driverFacade;
    private final CommandFacade commandFacade;
    private final ReactivePointCommandContext commandContext;
    private final ReactiveCommandHistoryStore historyStore;
    private final ReactiveMessageSender messageSender;
    private final CommandHistoryBuilder historyBuilder;

    @Override
    public Mono<String> call(Long tenantId, CommandCallBO request) {
        if (tenantId == null || tenantId <= 0 || request == null || request.getDeviceId() == null) {
            return Mono.error(new ServiceException("tenantId, deviceId and command are required"));
        }
        return deviceFacade.getByIdReactive(tenantId, request.getDeviceId())
                .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .flatMap(device -> validateDevice(device)
                        .then(resolveCommand(tenantId, device, request.getCommandId(), request.getCommandCode())
                                .switchIfEmpty(Mono.error(new NotFoundException("Command does not exist")))
                                .flatMap(command -> validateCommand(device, command)
                                        .then(driverFacade.getByIdReactive(tenantId, device.getDriverId())
                                                .switchIfEmpty(Mono.error(new ServiceException(
                                                        "No driver registered for this device"))))
                                        .flatMap(driver -> commandContext.activeOwner(tenantId, device.getId())
                                                .filter(owner -> owner.driverId() != null
                                                        && owner.driverId().equals(driver.getId())
                                                        && StringUtils.isNotBlank(owner.ownerNode())
                                                        && owner.fencingToken() != null && owner.fencingToken() > 0)
                                                .switchIfEmpty(Mono.error(new ServiceException(
                                                        "Device has no active driver owner")))
                                                .flatMap(owner -> persistAndPublish(tenantId, request, device, command,
                                                        driver, owner))))));
    }

    @Override
    public Mono<CommandHistoryVO> getByRecordId(Long tenantId, String recordId) {
        return historyStore.find(tenantId, recordId).map(historyBuilder::buildVOByDO);
    }

    @Override
    public Mono<OffsetPage<CommandHistoryVO>> list(Long tenantId, CommandHistoryQueryVO queryVO) {
        CommandHistoryQueryVO query = queryVO == null ? new CommandHistoryQueryVO() : queryVO;
        long offset = query.getOffset() == null ? 0L : query.getOffset();
        int limit = query.getLimit() == null ? 50 : query.getLimit();
        return historyStore.list(tenantId, parseId(query.getDeviceId(), "deviceId"),
                        parseId(query.getCommandId(), "commandId"), query.getCommandCode(), query.getStatus(),
                        offset, limit, query.getSort())
                .map(historyBuilder::buildVOPageByDOPage);
    }

    private Mono<FacadeCommandBO> resolveCommand(Long tenantId, FacadeDeviceBO device,
                                                   Long commandId, String commandCode) {
        if (commandId != null) return commandFacade.getById(tenantId, commandId);
        if (StringUtils.isBlank(commandCode)) return Mono.error(new ServiceException("Command id or code is required"));
        return commandFacade.list(new FacadeCommandOffsetQuery(tenantId, null, commandCode,
                        null, null, device.getProfileId(), null, null, device.getId(), 0, 1, java.util.List.of()))
                .flatMapMany(page -> reactor.core.publisher.Flux.fromIterable(page.items())).next();
    }

    private Mono<Void> validateDevice(FacadeDeviceBO device) {
        return EnableFlagEnum.DISABLE.equals(device.getEnableFlag())
                ? Mono.error(new ServiceException("Device is disabled")) : Mono.empty();
    }

    private Mono<Void> validateCommand(FacadeDeviceBO device, FacadeCommandBO command) {
        if (EnableFlagEnum.DISABLE.equals(command.getEnableFlag())) {
            return Mono.error(new ServiceException("Command is disabled"));
        }
        if (device.getProfileId() == null || !device.getProfileId().equals(command.getProfileId())) {
            return Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH));
        }
        return Mono.empty();
    }

    private Mono<String> persistAndPublish(Long tenantId, CommandCallBO request, FacadeDeviceBO device,
                                           FacadeCommandBO command, FacadeDriverBO driver,
                                           FacadeDeviceOwnerBO owner) {
        int timeout = command.getTimeout() == null || command.getTimeout() <= 0
                ? DEFAULT_COMMAND_TIMEOUT_SECONDS : command.getTimeout();
        String recordId = UuidV7.next().toString();
        Instant now = Instant.now();
        CommandHistoryDO history = new CommandHistoryDO();
        history.setRecordId(recordId);
        history.setTenantId(tenantId);
        history.setDeviceId(device.getId());
        history.setCommandId(command.getId());
        history.setCommandCode(command.getCommandCode());
        history.setParamValues(request.getParamValues() == null ? null : JsonUtil.toJsonString(request.getParamValues()));
        history.setStatus(PointCommandStatusEnum.PENDING);
        CommandHistorySourceEnum source = request.getSource() == null ? CommandHistorySourceEnum.HTTP : request.getSource();
        history.setSource(source);
        history.setSourceUserId(request.getSourceUserId());
        history.setOccurTime(local(now));
        history.setExpireTime(local(now.plusSeconds(timeout)));
        history.setSchemaVersion((short) 1);
        CommandCallDTO payload = CommandCallDTO.builder().recordId(recordId).tenantId(tenantId)
                .ownerNode(owner.ownerNode()).fencingToken(owner.fencingToken()).deviceId(device.getId())
                .commandId(command.getId()).commandCode(command.getCommandCode()).paramValues(request.getParamValues())
                .source(source).sourceUserId(request.getSourceUserId()).occurredAt(now).expireAt(now.plusSeconds(timeout))
                .schemaVersion(1).build();
        return historyStore.insert(history)
                .flatMap(saved -> messageSender.sendConfirmed(MqMessage.builder().topic(MqTopic.COMMAND)
                                .partitionKey(driver.getServiceName() + "." + owner.ownerNode()).payload(payload)
                                .header(MqHeaders.CORRELATION_ID, recordId)
                                .header(MqHeaders.TENANT_ID, String.valueOf(tenantId)).build())
                        .then(historyStore.markSent(tenantId, recordId, Instant.now()))
                        .flatMap(marked -> marked ? Mono.just(recordId)
                                : Mono.error(new ServiceException("Command disappeared before dispatch")))
                        .onErrorResume(error -> historyStore.markPublishFailed(tenantId, recordId,
                                        "BROKER_PUBLISH_FAILED", error.getMessage(), Instant.now())
                                .onErrorResume(markError -> {
                                    log.error("Failed to persist command publish failure, recordId={}", recordId, markError);
                                    return Mono.just(false);
                                }).then(Mono.error(new ServiceException(
                                        "Failed to route custom command to active driver owner", error)))));
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
}
