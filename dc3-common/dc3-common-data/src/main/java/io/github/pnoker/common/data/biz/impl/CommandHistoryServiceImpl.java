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

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.dal.CommandHistoryManager;
import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.builder.CommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.entity.common.Pages;
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
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitPublishConfirm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Business service implementation for custom command call operations.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHistoryServiceImpl implements CommandHistoryService {

    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 30;

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final CommandFacade commandFacade;

    private final RabbitTemplate rabbitTemplate;

    private final CommandHistoryManager commandHistoryManager;

    private final CommandHistoryBuilder commandHistoryBuilder;

    @Override
    public String call(Long tenantId, CommandCallBO entityBO) {
        FacadeCommandBO command = validateCommandScope(tenantId, entityBO.getDeviceId(), entityBO.getCommandId(),
                entityBO.getCommandCode());
        Long commandId = command.getId();

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityBO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        FacadeDeviceOwnerBO owner = requireActiveOwner(tenantId, entityBO.getDeviceId(), driver.getId());

        int timeoutSeconds = resolveCommandTimeout(command);

        String recordId = UUID.randomUUID().toString();
        LocalDateTime nowLocal = LocalDateTime.now();
        Instant now = Instant.now();

        CommandHistoryDO recordDO = new CommandHistoryDO();
        recordDO.setRecordId(recordId);
        recordDO.setTenantId(tenantId);
        recordDO.setDeviceId(entityBO.getDeviceId());
        recordDO.setCommandId(commandId);
        recordDO.setCommandCode(command.getCommandCode());
        recordDO.setParamValues(Objects.isNull(entityBO.getParamValues()) ? null : JsonUtil.toJsonString(entityBO.getParamValues()));
        recordDO.setStatus(PointCommandStatusEnum.PENDING);
        recordDO.setSource(CommandHistorySourceEnum.HTTP);
        recordDO.setOccurTime(nowLocal);
        recordDO.setExpireTime(nowLocal.plusSeconds(timeoutSeconds));
        recordDO.setSchemaVersion((short) 1);
        commandHistoryManager.save(recordDO);

        CommandCallDTO commandDTO = CommandCallDTO.builder()
                .recordId(recordId)
                .tenantId(tenantId)
                .ownerNode(owner.ownerNode())
                .fencingToken(owner.fencingToken())
                .deviceId(entityBO.getDeviceId())
                .commandId(commandId)
                .commandCode(command.getCommandCode())
                .paramValues(entityBO.getParamValues())
                .source(CommandHistorySourceEnum.HTTP)
                .occurredAt(now)
                .expireAt(now.plusSeconds(timeoutSeconds))
                .schemaVersion(1)
                .build();
        try {
            publishCommand(commandDTO, driver.getServiceName(), owner.ownerNode(), recordId);
        } catch (Exception e) {
            markPublishFailed(recordDO, e);
            throw new ServiceException("Failed to route custom command to active driver owner", e);
        }

        recordDO.setStatus(PointCommandStatusEnum.SENT);
        recordDO.setSendTime(LocalDateTime.now());
        commandHistoryManager.updateById(recordDO);

        return recordId;
    }

    @Override
    public CommandHistoryVO getByRecordId(Long tenantId, String recordId) {
        CommandHistoryDO entityDO = commandHistoryManager.lambdaQuery()
                .eq(Objects.nonNull(tenantId), CommandHistoryDO::getTenantId, tenantId)
                .eq(CommandHistoryDO::getRecordId, recordId)
                .one();
        return commandHistoryBuilder.buildVOByDO(entityDO);
    }

    @Override
    public Page<CommandHistoryVO> list(Long tenantId, CommandHistoryQueryVO queryVO) {
        LambdaQueryWrapper<CommandHistoryDO> wrapper = new LambdaQueryWrapper<CommandHistoryDO>()
                .eq(CommandHistoryDO::getTenantId, tenantId)
                .eq(Objects.nonNull(queryVO.getDeviceId()), CommandHistoryDO::getDeviceId, queryVO.getDeviceId())
                .eq(Objects.nonNull(queryVO.getCommandId()), CommandHistoryDO::getCommandId, queryVO.getCommandId())
                .eq(StringUtils.isNotBlank(queryVO.getCommandCode()), CommandHistoryDO::getCommandCode,
                        queryVO.getCommandCode())
                .eq(Objects.nonNull(queryVO.getStatus()), CommandHistoryDO::getStatus, queryVO.getStatus())
                .orderByDesc(CommandHistoryDO::getOccurTime);
        Page<CommandHistoryDO> page = commandHistoryManager.page(queryVO.toPage(), wrapper);
        return commandHistoryBuilder.buildVOPageByDOPage(page);
    }

    /**
     * Validate the device exists and is enabled within the tenant, then resolve and
     * validate the command, requiring the command share the device's profile.
     *
     * @param tenantId    tenant scope
     * @param deviceId    the device to validate
     * @param commandId   the command id, preferred when present
     * @param commandCode the command code, used as fallback
     * @return the resolved, enabled command
     */
    private FacadeCommandBO validateCommandScope(Long tenantId, Long deviceId, Long commandId, String commandCode) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
            throw new ServiceException("Device is disabled");
        }

        FacadeCommandBO command = resolveCommand(tenantId, device, commandId, commandCode);
        if (Objects.isNull(command)) {
            throw new NotFoundException("Command does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(command.getEnableFlag())) {
            throw new ServiceException("Command is disabled");
        }
        if (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), command.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return command;
    }

    /**
     * Resolve a command by id when present, otherwise by code within the device's profile.
     * Requires at least one of command id or code.
     *
     * @param tenantId    tenant scope
     * @param device      the device whose profile scopes the lookup
     * @param commandId   the command id, used when present
     * @param commandCode the command code, used as fallback
     * @return the resolved command, or {@code null} when none matches
     */
    private FacadeCommandBO resolveCommand(Long tenantId, FacadeDeviceBO device, Long commandId, String commandCode) {
        if (Objects.nonNull(commandId)) {
            return commandFacade.getById(tenantId, commandId);
        }
        if (StringUtils.isBlank(commandCode)) {
            throw new ServiceException("Command id or code is required");
        }

        Pages page = new Pages();
        page.setSize(1);
        FacadePage<FacadeCommandBO> commandPage = commandFacade.listByPage(FacadeCommandQuery.builder()
                .page(page)
                .tenantId(tenantId)
                .profileId(device.getProfileId())
                .commandCode(commandCode)
                .build());
        if (Objects.isNull(commandPage) || Objects.isNull(commandPage.getRecords()) || commandPage.getRecords().isEmpty()) {
            return null;
        }
        return commandPage.getRecords().get(0);
    }

    /**
     * Resolve the command timeout in seconds. The model has one unit only: seconds.
     *
     * @param command the command carrying the raw timeout
     * @return the timeout in seconds
     */
    private int resolveCommandTimeout(FacadeCommandBO command) {
        if (Objects.isNull(command) || Objects.isNull(command.getTimeout()) || command.getTimeout() <= 0) {
            return DEFAULT_COMMAND_TIMEOUT_SECONDS;
        }
        return command.getTimeout();
    }

    /**
     * Publish a command call to the driver via RabbitMQ, correlating by record id.
     *
     * @param dto         the command call payload
     * @param serviceName the target driver's service name
     * @param recordId    the command history record id, used as the correlation id
     */
    private void publishCommand(CommandCallDTO dto, String serviceName, String ownerNode, String recordId) {
        CorrelationData correlationData = new CorrelationData(recordId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + serviceName + "." + ownerNode, dto, correlationData);
        RabbitPublishConfirm.awaitRouted(correlationData, Duration.ofSeconds(5));
    }

    private void markPublishFailed(CommandHistoryDO recordDO, Exception cause) {
        recordDO.setStatus(PointCommandStatusEnum.FAILED);
        recordDO.setErrorCode("BROKER_PUBLISH_FAILED");
        recordDO.setErrorMessage(cause.getMessage());
        recordDO.setFinishTime(LocalDateTime.now());
        commandHistoryManager.updateById(recordDO);
    }

    private FacadeDeviceOwnerBO requireActiveOwner(Long tenantId, Long deviceId, Long driverId) {
        FacadeDeviceOwnerBO owner = deviceFacade.getActiveOwner(tenantId, deviceId);
        if (Objects.isNull(owner) || !Objects.equals(owner.driverId(), driverId)
                || Objects.isNull(owner.ownerNode()) || owner.ownerNode().isBlank()
                || Objects.isNull(owner.fencingToken()) || owner.fencingToken() <= 0) {
            throw new ServiceException("Device has no active driver owner");
        }
        return owner;
    }

}
