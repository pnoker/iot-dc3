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
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final CommandFacade commandFacade;

    private final RabbitTemplate rabbitTemplate;

    private final CommandHistoryManager commandHistoryManager;

    private final EntityStateMapper entityStateMapper;

    @Override
    public String call(Long tenantId, CommandCallVO entityVO) {
        validateCommandScope(tenantId, entityVO.getDeviceId(), entityVO.getCommandId());

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            throw new ServiceException("No driver registered for this device");
        }
        checkDriverOnline(tenantId, driver.getId());

        FacadeCommandBO command = commandFacade.getById(tenantId, entityVO.getCommandId());

        String recordId = UUID.randomUUID().toString();
        LocalDateTime nowLocal = LocalDateTime.now();

        CommandHistoryDO recordDO = new CommandHistoryDO();
        recordDO.setRecordId(recordId);
        recordDO.setTenantId(tenantId);
        recordDO.setDeviceId(entityVO.getDeviceId());
        recordDO.setCommandId(entityVO.getCommandId());
        recordDO.setCommandCode(command.getCommandCode());
        recordDO.setParamValues(Objects.isNull(entityVO.getParamValues()) ? null : JsonUtil.toJsonString(entityVO.getParamValues()));
        recordDO.setStatus(PointCommandStatusEnum.PENDING.getCode());
        recordDO.setSource(CommandHistorySourceEnum.HTTP.getCode());
        recordDO.setOccurTime(nowLocal);
        recordDO.setExpireTime(nowLocal.plusSeconds(30));
        recordDO.setSchemaVersion((short) 1);
        commandHistoryManager.save(recordDO);

        publishCommand(CommandCallDTO.builder()
                .recordId(recordId)
                .tenantId(tenantId)
                .deviceId(entityVO.getDeviceId())
                .commandId(entityVO.getCommandId())
                .commandCode(command.getCommandCode())
                .paramValues(entityVO.getParamValues())
                .source(CommandHistorySourceEnum.HTTP.getCode())
                .occurredAt(java.time.Instant.now())
                .expireAt(java.time.Instant.now().plusSeconds(30))
                .schemaVersion(1)
                .build(), driver.getServiceName(), recordId);

        recordDO.setStatus(PointCommandStatusEnum.SENT.getCode());
        recordDO.setSendTime(LocalDateTime.now());
        commandHistoryManager.updateById(recordDO);

        return recordId;
    }

    @Override
    public CommandHistoryDO getByRecordId(String recordId) {
        return commandHistoryManager.lambdaQuery()
                .eq(CommandHistoryDO::getRecordId, recordId)
                .one();
    }

    @Override
    public Page<CommandHistoryDO> list(Long tenantId, CommandHistoryQueryVO queryVO) {
        LambdaQueryWrapper<CommandHistoryDO> wrapper = new LambdaQueryWrapper<CommandHistoryDO>()
                .eq(CommandHistoryDO::getTenantId, tenantId)
                .eq(Objects.nonNull(queryVO.getDeviceId()), CommandHistoryDO::getDeviceId, queryVO.getDeviceId())
                .eq(Objects.nonNull(queryVO.getCommandId()), CommandHistoryDO::getCommandId, queryVO.getCommandId())
                .eq(Objects.nonNull(queryVO.getStatus()), CommandHistoryDO::getStatus, queryVO.getStatus())
                .orderByDesc(CommandHistoryDO::getOccurTime);
        return commandHistoryManager.page(queryVO.toPage(), wrapper);
    }

    private void checkDriverOnline(Long tenantId, Long driverId) {
        EntityStateDO driverState = entityStateMapper.selectOne(
                new LambdaQueryWrapper<EntityStateDO>()
                        .eq(EntityStateDO::getTenantId, tenantId)
                        .eq(EntityStateDO::getEntityTypeFlag, EntityTypeFlagEnum.DRIVER.getIndex())
                        .eq(EntityStateDO::getEntityId, driverId));
        if (Objects.isNull(driverState) || !EntityStatusEnum.ONLINE.getIndex().equals(driverState.getStateFlag())) {
            throw new ServiceException("Driver is offline");
        }
    }

    private void validateCommandScope(Long tenantId, Long deviceId, Long commandId) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
            throw new ServiceException("Device is disabled");
        }

        FacadeCommandBO command = commandFacade.getById(tenantId, commandId);
        if (Objects.isNull(command)) {
            throw new NotFoundException("Command does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(command.getEnableFlag())) {
            throw new ServiceException("Command is disabled");
        }
        if (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), command.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
    }

    private void publishCommand(CommandCallDTO dto, String serviceName, String recordId) {
        CorrelationData correlationData = new CorrelationData(recordId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + serviceName, dto, correlationData);
    }

}
