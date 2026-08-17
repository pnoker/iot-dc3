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
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.dal.PointCommandHistoryManager;
import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.data.entity.builder.PointCommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
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
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.utils.RabbitPublishConfirm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Business service implementation for point command operations.
 * <p>
 * Validates command scope, checks driver online status, persists the command,
 * publishes to the driver via RabbitMQ, and returns a {@code commandId} that
 * callers can use to poll for the terminal result.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandServiceImpl implements PointCommandService, PointCommandHistoryService {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    private final RabbitTemplate rabbitTemplate;

    private final PointCommandHistoryManager pointCommandHistoryManager;

    private final PointCommandHistoryBuilder pointCommandHistoryBuilder;

    private final PointCommandValidator pointCommandValidator;

    @Override
    public String read(Long tenantId, PointCommandReadBO entityBO) {
        validateCommandScope(tenantId, entityBO.getDeviceId(), entityBO.getPointId());

        // Idempotency: if caller supplied a commandId that already exists, return it
        String existing = checkExistingCommand(entityBO.getCommandId());
        if (Objects.nonNull(existing)) {
            return existing;
        }

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityBO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        FacadeDeviceOwnerBO owner = requireActiveOwner(tenantId, entityBO.getDeviceId(), driver.getId());

        String commandId = resolveCommandId(entityBO.getCommandId());
        LocalDateTime nowLocal = LocalDateTime.now();

        PointCommandHistoryDO commandDO = new PointCommandHistoryDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.READ);
        commandDO.setDeviceId(entityBO.getDeviceId());
        commandDO.setPointId(entityBO.getPointId());
        commandDO.setStatus(PointCommandStatusEnum.PENDING);
        commandDO.setSource(PointCommandSourceEnum.HTTP);
        commandDO.setOccurTime(nowLocal);
        commandDO.setExpireTime(nowLocal.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandHistoryManager.save(commandDO);

        try {
            publishCommand(PointCommandDTO.ofRead(commandId, tenantId, owner.ownerNode(), owner.fencingToken(),
                    entityBO.getDeviceId(), entityBO.getPointId()), driver.getServiceName(), owner.ownerNode(), commandId);
        } catch (Exception e) {
            markPublishFailed(commandDO, e);
            throw new ServiceException("Failed to route point command to active driver owner", e);
        }

        commandDO.setStatus(PointCommandStatusEnum.SENT);
        commandDO.setSendTime(LocalDateTime.now());
        pointCommandHistoryManager.updateById(commandDO);

        return commandId;
    }

    @Override
    public String write(Long tenantId, PointCommandWriteBO entityBO) {
        validateWriteScope(tenantId, entityBO.getDeviceId(), entityBO.getPointId());

        // Idempotency: if caller supplied a commandId that already exists, return it
        String existing = checkExistingCommand(entityBO.getCommandId());
        if (Objects.nonNull(existing)) {
            return existing;
        }

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityBO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        FacadeDeviceOwnerBO owner = requireActiveOwner(tenantId, entityBO.getDeviceId(), driver.getId());

        pointCommandValidator.validateWriteValue(entityBO.getValue());

        String commandId = resolveCommandId(entityBO.getCommandId());
        LocalDateTime nowLocal = LocalDateTime.now();

        PointCommandHistoryDO commandDO = new PointCommandHistoryDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.WRITE);
        commandDO.setDeviceId(entityBO.getDeviceId());
        commandDO.setPointId(entityBO.getPointId());
        commandDO.setRequestValue(entityBO.getValue());
        commandDO.setStatus(PointCommandStatusEnum.PENDING);
        commandDO.setSource(PointCommandSourceEnum.HTTP);
        commandDO.setOccurTime(nowLocal);
        commandDO.setExpireTime(nowLocal.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandHistoryManager.save(commandDO);

        try {
            publishCommand(PointCommandDTO.ofWrite(commandId, tenantId, owner.ownerNode(), owner.fencingToken(),
                            entityBO.getDeviceId(), entityBO.getPointId(), entityBO.getValue()),
                    driver.getServiceName(), owner.ownerNode(), commandId);
        } catch (Exception e) {
            markPublishFailed(commandDO, e);
            throw new ServiceException("Failed to route point command to active driver owner", e);
        }

        commandDO.setStatus(PointCommandStatusEnum.SENT);
        commandDO.setSendTime(LocalDateTime.now());
        pointCommandHistoryManager.updateById(commandDO);

        return commandId;
    }

    @Override
    public PointCommandHistoryVO getByCommandId(Long tenantId, String commandId) {
        PointCommandHistoryDO entityDO = pointCommandHistoryManager.lambdaQuery()
                .eq(Objects.nonNull(tenantId), PointCommandHistoryDO::getTenantId, tenantId)
                .eq(PointCommandHistoryDO::getCommandId, commandId)
                .one();
        return pointCommandHistoryBuilder.buildVOByDO(entityDO);
    }

    @Override
    public Page<PointCommandHistoryVO> list(Long tenantId, PointCommandHistoryQueryVO queryVO) {
        LambdaQueryWrapper<PointCommandHistoryDO> wrapper = new LambdaQueryWrapper<PointCommandHistoryDO>()
                .eq(PointCommandHistoryDO::getTenantId, tenantId)
                .eq(Objects.nonNull(queryVO.getDeviceId()), PointCommandHistoryDO::getDeviceId, queryVO.getDeviceId())
                .eq(Objects.nonNull(queryVO.getPointId()), PointCommandHistoryDO::getPointId, queryVO.getPointId())
                .eq(Objects.nonNull(queryVO.getStatus()), PointCommandHistoryDO::getStatus, queryVO.getStatus())
                .eq(Objects.nonNull(queryVO.getType()), PointCommandHistoryDO::getType, queryVO.getType())
                .orderByDesc(PointCommandHistoryDO::getOccurTime);
        Page<PointCommandHistoryDO> page = pointCommandHistoryManager.page(queryVO.toPage(), wrapper);
        return pointCommandHistoryBuilder.buildVOPageByDOPage(page);
    }

    /**
     * Validate the device and point exist within the tenant, are enabled, and share a
     * profile.
     *
     * @param tenantId tenant scope
     * @param deviceId the device to validate
     * @param pointId  the point to validate
     */
    private void validateCommandScope(Long tenantId, Long deviceId, Long pointId) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
            throw new ServiceException("Device is disabled");
        }

        FacadePointBO point = pointFacade.getById(tenantId, pointId);
        if (Objects.isNull(point)) {
            throw new NotFoundException("Point does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(point.getEnableFlag())) {
            throw new ServiceException("Point is disabled");
        }
        if (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), point.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
    }

    /**
     * Validate the command scope and additionally require the point be writable
     * (write-only or read-write).
     *
     * @param tenantId tenant scope
     * @param deviceId the device to validate
     * @param pointId  the point to validate for write access
     */
    private void validateWriteScope(Long tenantId, Long deviceId, Long pointId) {
        validateCommandScope(tenantId, deviceId, pointId);
        FacadePointBO point = pointFacade.getById(tenantId, pointId);
        if (!RwTypeEnum.WRITE_ONLY.equals(point.getRwFlag()) && !RwTypeEnum.READ_WRITE.equals(point.getRwFlag())) {
            throw new ServiceException("Point is not writable");
        }
    }

    /**
     * Check whether a caller-supplied commandId already exists.
     *
     * @param commandId the caller-supplied command id, may be null or blank
     * @return the existing commandId, or null if not provided or not found
     */
    private String checkExistingCommand(String commandId) {
        if (Objects.isNull(commandId) || commandId.isBlank()) {
            return null;
        }
        PointCommandHistoryVO existing = getByCommandId(commandId);
        return Objects.nonNull(existing) ? existing.getCommandId() : null;
    }

    /**
     * Resolve the commandId to use: caller-supplied, or generate a new UUID.
     */
    private String resolveCommandId(String callerCommandId) {
        if (Objects.nonNull(callerCommandId) && !callerCommandId.isBlank()) {
            return callerCommandId;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Publish a point command DTO to the driver via RabbitMQ.
     */
    private void publishCommand(PointCommandDTO dto, String serviceName, String ownerNode, String commandId) {
        CorrelationData correlationData = new CorrelationData(commandId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND,
                RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + serviceName + "." + ownerNode, dto, correlationData);
        RabbitPublishConfirm.awaitRouted(correlationData, Duration.ofSeconds(5));
    }

    private void markPublishFailed(PointCommandHistoryDO commandDO, Exception cause) {
        commandDO.setStatus(PointCommandStatusEnum.FAILED);
        commandDO.setErrorCode("BROKER_PUBLISH_FAILED");
        commandDO.setErrorMessage(cause.getMessage());
        commandDO.setFinishTime(LocalDateTime.now());
        pointCommandHistoryManager.updateById(commandDO);
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
