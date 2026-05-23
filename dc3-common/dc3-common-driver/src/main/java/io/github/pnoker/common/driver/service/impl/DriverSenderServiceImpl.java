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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Implements point-value dispatch to the data center via RabbitMQ.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverSenderServiceImpl implements DriverSenderService {

    private final DriverProperties driverProperties;

    private final DriverMetadata driverMetadata;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void driverStateSender(DriverStateDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_STATE,
                RabbitConstant.ROUTING_DRIVER_STATE_PREFIX + driverProperties.getService(), entityDTO);
    }

    @Override
    public void deviceStateSender(DeviceStateDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_STATE,
                RabbitConstant.ROUTING_DEVICE_STATE_PREFIX + driverProperties.getService(), entityDTO);
    }

    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status) {
        sendDeviceStatus(deviceId, status, driverProperties.getHealth().getDevice().getTimeout(),
                driverProperties.getHealth().getDevice().getTimeoutUnit(), null);
    }

    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit) {
        sendDeviceStatus(deviceId, status, timeout, timeoutUnit, null);
    }

    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit,
                                   String stateDescription) {
        sendDeviceStatus(deviceId, status, timeout, timeoutUnit, stateDescription);
    }

    @Override
    public void driverAlarmSender(String message) {
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.isNull(driver)) {
            log.warn("Driver not registered yet; drop alarm: {}", message);
            return;
        }
        DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                .tenantId(driver.getTenantId())
                .driverId(driver.getId())
                .message(message)
                .build();
        log.info("Report driver alarm: {}", message);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_ALARM,
                RabbitConstant.ROUTING_DRIVER_ALARM_PREFIX + driverProperties.getService(), alarm);
    }

    @Override
    public void deviceAlarmSender(Long deviceId, String message) {
        if (Objects.isNull(deviceId)) {
            return;
        }
        DeviceAlarmDTO alarm = DeviceAlarmDTO.builder()
                .deviceId(deviceId)
                .message(message)
                .build();
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.nonNull(driver)) {
            alarm.setDriverId(driver.getId());
            alarm.setTenantId(driver.getTenantId());
        }
        log.info("Report device alarm: deviceId={}, message={}", deviceId, message);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_ALARM,
                RabbitConstant.ROUTING_DEVICE_ALARM_PREFIX + driverProperties.getService(), alarm);
    }

    @Override
    public void pointValueSender(PointValue entityDTO) {
        if (Objects.nonNull(entityDTO)) {
            DriverBO driver = driverMetadata.getDriver();
            if (Objects.nonNull(driver)) {
                if (Objects.isNull(entityDTO.getDriverId())) {
                    entityDTO.setDriverId(driver.getId());
                }
                if (Objects.isNull(entityDTO.getTenantId())) {
                    entityDTO.setTenantId(driver.getTenantId());
                }
            } else {
                log.warn(
                        "DriverMetadata has no registered driver yet; point value will be published without driverId/tenantId");
            }
            log.info("Send point value: {}", JsonUtil.toJsonString(entityDTO));
            rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_VALUE,
                    RabbitConstant.ROUTING_POINT_VALUE_PREFIX + driverProperties.getService(), entityDTO);
        }
    }

    @Override
    public void pointValueSender(List<PointValue> entityDTOList) {
        if (Objects.nonNull(entityDTOList)) {
            entityDTOList.forEach(this::pointValueSender);
        }
    }

    @Override
    public void pointCommandResultSender(PointCommandResultDTO resultDTO) {
        if (Objects.isNull(resultDTO)) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_POINT_COMMAND_RESULT,
                RabbitConstant.ROUTING_POINT_COMMAND_RESULT_PREFIX + driverProperties.getService(), resultDTO);
    }

    @Override
    public void commandResultSender(CommandCallResultDTO resultDTO) {
        if (Objects.isNull(resultDTO)) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND_RESULT,
                RabbitConstant.ROUTING_COMMAND_RESULT_PREFIX + driverProperties.getService(), resultDTO);
    }

    @Override
    public void eventReportSender(EventReportDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + driverProperties.getService(), entityDTO);
    }

    private void sendDeviceStatus(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit,
                                  String stateDescription) {
        DeviceStateDTO deviceState = new DeviceStateDTO(deviceId, status, timeout, timeoutUnit);
        if (Objects.nonNull(stateDescription)) {
            deviceState.setStateDescription(stateDescription);
        }
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.nonNull(driver)) {
            deviceState.setDriverId(driver.getId());
            deviceState.setTenantId(driver.getTenantId());
        } else {
            log.warn(
                    "DriverMetadata has no registered driver yet; device status will be published without driverId/tenantId");
        }
        log.info("Report device state: {}, deviceId={}", status.getCode(), deviceId);
        deviceStateSender(deviceState);
    }

}
