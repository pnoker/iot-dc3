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
import io.github.pnoker.common.data.entity.builder.PointCommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.data.validator.PointCommandValidator;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
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
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final EntityStateMapper entityStateMapper;

    private final PointCommandValidator pointCommandValidator;

    @Override
    public String read(Long tenantId, PointCommandReadVO entityVO) {
        validateCommandScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        // Idempotency: if caller supplied a commandId that already exists, return it
        String existing = checkExistingCommand(entityVO.getCommandId());
        if (Objects.nonNull(existing)) {
            return existing;
        }

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        checkDriverOnline(tenantId, driver.getId());

        String commandId = resolveCommandId(entityVO.getCommandId());
        LocalDateTime nowLocal = LocalDateTime.now();

        PointCommandHistoryDO commandDO = new PointCommandHistoryDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.READ.getCode());
        commandDO.setDeviceId(entityVO.getDeviceId());
        commandDO.setPointId(entityVO.getPointId());
        commandDO.setStatus(PointCommandStatusEnum.PENDING.getCode());
        commandDO.setSource(PointCommandSourceEnum.HTTP.getCode());
        commandDO.setOccurTime(nowLocal);
        commandDO.setExpireTime(nowLocal.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandHistoryManager.save(commandDO);

        publishCommand(PointCommandDTO.ofRead(commandId, tenantId, entityVO.getDeviceId(),
                entityVO.getPointId()), driver.getServiceName(), commandId);

        commandDO.setStatus(PointCommandStatusEnum.SENT.getCode());
        commandDO.setSendTime(LocalDateTime.now());
        pointCommandHistoryManager.updateById(commandDO);

        return commandId;
    }

    @Override
    public String write(Long tenantId, PointCommandWriteVO entityVO) {
        validateWriteScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        // Idempotency: if caller supplied a commandId that already exists, return it
        String existing = checkExistingCommand(entityVO.getCommandId());
        if (Objects.nonNull(existing)) {
            return existing;
        }

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        checkDriverOnline(tenantId, driver.getId());

        FacadePointBO point = pointFacade.getById(tenantId, entityVO.getPointId());
        pointCommandValidator.validateWriteValue(entityVO.getValue(), Objects.nonNull(point) ? point.getPointExt() : null);

        String commandId = resolveCommandId(entityVO.getCommandId());
        LocalDateTime nowLocal = LocalDateTime.now();

        PointCommandHistoryDO commandDO = new PointCommandHistoryDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.WRITE.getCode());
        commandDO.setDeviceId(entityVO.getDeviceId());
        commandDO.setPointId(entityVO.getPointId());
        commandDO.setRequestValue(entityVO.getValue());
        commandDO.setStatus(PointCommandStatusEnum.PENDING.getCode());
        commandDO.setSource(PointCommandSourceEnum.HTTP.getCode());
        commandDO.setOccurTime(nowLocal);
        commandDO.setExpireTime(nowLocal.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandHistoryManager.save(commandDO);

        publishCommand(PointCommandDTO.ofWrite(commandId, tenantId, entityVO.getDeviceId(),
                entityVO.getPointId(), entityVO.getValue()), driver.getServiceName(), commandId);

        commandDO.setStatus(PointCommandStatusEnum.SENT.getCode());
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
                .eq(Objects.nonNull(queryVO.getStatus()), PointCommandHistoryDO::getStatus,
                        Objects.nonNull(queryVO.getStatus()) ? queryVO.getStatus().getCode() : null)
                .eq(Objects.nonNull(queryVO.getType()), PointCommandHistoryDO::getType, queryVO.getType())
                .orderByDesc(PointCommandHistoryDO::getOccurTime);
        Page<PointCommandHistoryDO> page = pointCommandHistoryManager.page(queryVO.toPage(), wrapper);
        return pointCommandHistoryBuilder.buildVOPageByDOPage(page);
    }

    private void checkDriverOnline(Long tenantId, Long driverId) {
        EntityStateDO driverState = entityStateMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EntityStateDO>()
                        .eq(EntityStateDO::getTenantId, tenantId)
                        .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                        .eq(EntityStateDO::getEntityId, driverId));
        if (Objects.isNull(driverState) || !EntityStatusEnum.ONLINE.getIndex().equals(driverState.getStateFlag())) {
            throw new ServiceException("Driver is offline");
        }
    }

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
    private void publishCommand(PointCommandDTO dto, String serviceName, String commandId) {
        CorrelationData correlationData = new CorrelationData(commandId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND,
                RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + serviceName, dto, correlationData);
    }

}
