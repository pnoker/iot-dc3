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
import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.enums.DeviceCommandTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for point value command operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointValueCommandServiceImpl implements PointValueCommandService {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void read(Long tenantId, PointValueReadVO entityVO) {
        validateCommandScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            return;
        }

        DeviceCommandDTO.DeviceRead deviceRead = new DeviceCommandDTO.DeviceRead(entityVO.getDeviceId(),
                entityVO.getPointId());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.READ,
                JsonUtil.toJsonString(deviceRead));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + driver.getServiceName(), deviceCommandDTO);
    }

    @Override
    public void write(Long tenantId, PointValueWriteVO entityVO) {
        validateCommandScope(tenantId, entityVO.getDeviceId(), entityVO.getPointId());

        FacadeDriverBO driver = driverFacade.getByDeviceId(tenantId, entityVO.getDeviceId());
        if (Objects.isNull(driver)) {
            return;
        }

        DeviceCommandDTO.DeviceWrite deviceWrite = new DeviceCommandDTO.DeviceWrite(entityVO.getDeviceId(),
                entityVO.getPointId(), entityVO.getValue());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.WRITE,
                JsonUtil.toJsonString(deviceWrite));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + driver.getServiceName(), deviceCommandDTO);
    }

    private void validateCommandScope(Long tenantId, Long deviceId, Long pointId) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }

        FacadePointBO point = pointFacade.getById(tenantId, pointId);
        if (Objects.isNull(point)) {
            throw new NotFoundException("Point does not exist");
        }
        if (Objects.isNull(device.getProfileIds()) || !device.getProfileIds().contains(point.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
    }

}
