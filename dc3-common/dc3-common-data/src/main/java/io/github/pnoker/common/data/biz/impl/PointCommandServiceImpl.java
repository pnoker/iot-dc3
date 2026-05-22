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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.dal.PointCommandManager;
import io.github.pnoker.common.data.entity.model.PointCommandDO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
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
 * Business service implementation for point command operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandServiceImpl implements PointCommandService {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    private final RabbitTemplate rabbitTemplate;

    private final PointCommandManager pointCommandManager;

    @Override
    public void read(Long tenantId, PointCommandReadVO entityVO) {
        validateCommandScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            return;
        }

        String commandId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        PointCommandDO commandDO = new PointCommandDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.READ.getCode());
        commandDO.setDeviceId(entityVO.getDeviceId());
        commandDO.setPointId(entityVO.getPointId());
        commandDO.setStatus(PointCommandStatusEnum.PENDING.getCode());
        commandDO.setSource(PointCommandSourceEnum.HTTP.getCode());
        commandDO.setOccurredAt(now);
        commandDO.setExpireAt(now.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandManager.save(commandDO);

        PointCommandDTO.PointRead pointRead = new PointCommandDTO.PointRead(entityVO.getDeviceId(),
                entityVO.getPointId());
        PointCommandDTO pointCommandDTO = new PointCommandDTO(PointCommandTypeEnum.READ,
                JsonUtil.toJsonString(pointRead));
        pointCommandDTO.setCommandId(commandId);
        pointCommandDTO.setTenantId(tenantId);
        CorrelationData correlationData = new CorrelationData(commandId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND,
                RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + driver.getServiceName(), pointCommandDTO,
                correlationData);

        commandDO.setStatus(PointCommandStatusEnum.SENT.getCode());
        commandDO.setSentAt(LocalDateTime.now());
        pointCommandManager.updateById(commandDO);
    }

    @Override
    public void write(Long tenantId, PointCommandWriteVO entityVO) {
        validateWriteScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            return;
        }

        String commandId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        PointCommandDO commandDO = new PointCommandDO();
        commandDO.setCommandId(commandId);
        commandDO.setTenantId(tenantId);
        commandDO.setType(PointCommandTypeEnum.WRITE.getCode());
        commandDO.setDeviceId(entityVO.getDeviceId());
        commandDO.setPointId(entityVO.getPointId());
        commandDO.setRequestValue(entityVO.getValue());
        commandDO.setStatus(PointCommandStatusEnum.PENDING.getCode());
        commandDO.setSource(PointCommandSourceEnum.HTTP.getCode());
        commandDO.setOccurredAt(now);
        commandDO.setExpireAt(now.plusSeconds(10));
        commandDO.setSchemaVersion((short) 1);
        pointCommandManager.save(commandDO);

        PointCommandDTO.PointWrite pointWrite = new PointCommandDTO.PointWrite(entityVO.getDeviceId(),
                entityVO.getPointId(), entityVO.getValue());
        PointCommandDTO pointCommandDTO = new PointCommandDTO(PointCommandTypeEnum.WRITE,
                JsonUtil.toJsonString(pointWrite));
        pointCommandDTO.setCommandId(commandId);
        pointCommandDTO.setTenantId(tenantId);
        CorrelationData correlationData = new CorrelationData(commandId);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND,
                RabbitConstant.ROUTING_POINT_COMMAND_PREFIX + driver.getServiceName(), pointCommandDTO,
                correlationData);

        commandDO.setStatus(PointCommandStatusEnum.SENT.getCode());
        commandDO.setSentAt(LocalDateTime.now());
        pointCommandManager.updateById(commandDO);
    }

    @Override
    public PointCommandDO getByCommandId(String commandId) {
        return pointCommandManager.lambdaQuery()
                .eq(PointCommandDO::getCommandId, commandId)
                .one();
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
        if (Objects.isNull(device.getProfileIds()) || !device.getProfileIds().contains(point.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
    }

    private void validateWriteScope(Long tenantId, Long deviceId, Long pointId) {
        validateCommandScope(tenantId, deviceId, pointId);
        FacadePointBO point = pointFacade.getById(tenantId, pointId);
        if (!RwFlagEnum.W.equals(point.getRwFlag()) && !RwFlagEnum.RW.equals(point.getRwFlag())) {
            throw new ServiceException("Point is not writable");
        }
    }

}
