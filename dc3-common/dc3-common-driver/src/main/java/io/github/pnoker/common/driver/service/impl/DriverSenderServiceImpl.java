/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverSenderServiceImpl implements DriverSenderService {

    private final DriverProperties driverProperties;
    private final RabbitTemplate rabbitTemplate;

    public DriverSenderServiceImpl(DriverProperties driverProperties, RabbitTemplate rabbitTemplate) {
        this.driverProperties = driverProperties;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void driverEventSender(DriverEventDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + driverProperties.getService(),
                entityDTO
        );
    }

    @Override
    public void deviceEventSender(DeviceEventDTO entityDTO) {
        if (!Objects.nonNull(entityDTO)) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + driverProperties.getService(),
                entityDTO
        );
    }

    @Override
    public void deviceStatusSender(Long deviceId, DeviceStatusEnum status) {
        sendDeviceStatus(deviceId, status, 15, TimeUnit.MINUTES);
    }

    @Override
    public void deviceStatusSender(Long deviceId, DeviceStatusEnum status, int timeOut, TimeUnit timeUnit) {
        sendDeviceStatus(deviceId, status, timeOut, timeUnit);
    }

    @Override
    public void pointValueSender(PointValue entityDTO) {
        if (Objects.nonNull(entityDTO)) {
            log.info("Send point value: {}", JsonUtil.toJsonString(entityDTO));
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_VALUE,
                    RabbitConstant.ROUTING_POINT_VALUE_PREFIX + driverProperties.getService(),
                    entityDTO
            );
        }
    }

    @Override
    public void pointValueSender(List<PointValue> entityDTOList) {
        if (Objects.nonNull(entityDTOList)) {
            entityDTOList.forEach(this::pointValueSender);
        }
    }

    private void sendDeviceStatus(Long deviceId, DeviceStatusEnum status, int timeOut, TimeUnit timeUnit) {
        DeviceEventDTO.DeviceStatus deviceStatus = new DeviceEventDTO.DeviceStatus(deviceId, status, timeOut, timeUnit);
        DeviceEventDTO deviceEventDTO = new DeviceEventDTO(DeviceEventTypeEnum.HEARTBEAT, JsonUtil.toJsonString(deviceStatus));
        log.info("Report device event: {}, event content: {}", deviceEventDTO.getType().getCode(), JsonUtil.toJsonString(deviceEventDTO));
        deviceEventSender(deviceEventDTO);
    }

}
