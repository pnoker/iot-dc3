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

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.driver.buffer.BufferService;
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
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements point-value dispatch to the data center via RabbitMQ.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverSenderServiceImpl implements DriverSenderService {

    private static final int POINT_VALUE_SCHEMA_VERSION = 1;

    private final AtomicLong pointValueSequence = new AtomicLong();

    /**
     * Tenant-scoped driver configuration (service name, health timeouts, etc.).
     */
    private final DriverProperties driverProperties;

    /**
     * Runtime driver registration context (driver id / tenant id) stamped onto outbound payloads.
     */
    private final DriverMetadata driverMetadata;

    /**
     * Broker-neutral publisher used to deliver every outbound message to the data center.
     */
    private final MessageSender messageSender;

    /**
     * Durable point-value outbox. Values are committed locally before RabbitMQ publication and are
     * republished until broker confirmation removes them.
     */
    private final BufferService bufferService;

    /**
     * Publish the driver's lifecycle state to the state exchange.
     *
     * @param entityDTO driver state payload
     */
    @Override
    public void driverStateSender(DriverStateDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        messageSender.send(MqMessage.of(MqTopic.STATE, "driver." + driverProperties.getService(), entityDTO));
    }

    /**
     * Publish a device lifecycle state to the state exchange.
     *
     * @param entityDTO device state payload
     */
    @Override
    public void deviceStateSender(DeviceStateDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        messageSender.send(MqMessage.of(MqTopic.STATE, "device." + driverProperties.getService(), entityDTO));
    }

    /**
     * Report a device status using the configured default health timeout.
     */
    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status) {
        sendDeviceStatus(deviceId, status, driverProperties.getHealth().getDevice().getTimeout(),
                driverProperties.getHealth().getDevice().getTimeoutUnit(), null);
    }

    /**
     * Report a device status with an explicit lease timeout.
     */
    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit) {
        sendDeviceStatus(deviceId, status, timeout, timeoutUnit, null);
    }

    /**
     * Report a device status with an explicit lease timeout and structured description.
     */
    @Override
    public void deviceStatusSender(Long deviceId, EntityStatusEnum status, int timeout, TimeUnit timeoutUnit,
                                   String stateDescription) {
        sendDeviceStatus(deviceId, status, timeout, timeoutUnit, stateDescription);
    }

    /**
     * Publish a driver-scoped alarm enriched with tenant/driver context from the registered driver.
     *
     * @param message human-readable alarm message
     */
    @Override
    public void driverAlarmSender(String message) {
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.isNull(driver)) {
            log.warn("Driver alarm publish skipped, reason=driverNotRegistered, messageLength={}",
                    Objects.nonNull(message) ? message.length() : 0);
            return;
        }
        DriverAlarmDTO alarm = DriverAlarmDTO.builder()
                .tenantId(driver.getTenantId())
                .driverId(driver.getId())
                .message(message)
                .build();
        log.info("Driver alarm published, tenantId={}, driverId={}, messageLength={}",
                driver.getTenantId(), driver.getId(), Objects.nonNull(message) ? message.length() : 0);
        messageSender.send(MqMessage.of(MqTopic.ALARM, "driver." + driverProperties.getService(), alarm));
    }

    /**
     * Publish a device-scoped alarm, stamping tenant/driver context when the driver is registered.
     *
     * @param deviceId target device
     * @param message  human-readable alarm message
     */
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
        log.info("Device alarm published, tenantId={}, driverId={}, deviceId={}, messageLength={}",
                alarm.getTenantId(), alarm.getDriverId(), deviceId, Objects.nonNull(message) ? message.length() : 0);
        messageSender.send(MqMessage.of(MqTopic.ALARM, "device." + driverProperties.getService(), alarm));
    }

    /**
     * Publish a single point value, filling tenant/driver context and attaching a publisher-confirm correlation.
     *
     * @param entityDTO point value payload
     */
    @Override
    public void pointValueSender(PointValue entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.isNull(driver)) {
            log.error("Reject point value before driver registration, deviceId={}, pointId={}",
                    entityDTO.getDeviceId(), entityDTO.getPointId());
            return;
        }
        if (!stampPointValue(entityDTO, driver)) {
            return;
        }

        bufferService.publish(entityDTO, driverProperties.getService());
    }

    private boolean stampPointValue(PointValue pointValue, DriverBO driver) {
        if (Objects.isNull(pointValue.getDriverId())) {
            pointValue.setDriverId(driver.getId());
        }
        if (Objects.isNull(pointValue.getTenantId())) {
            pointValue.setTenantId(driver.getTenantId());
        }
        Long fencingToken = driverMetadata.getFencingToken(pointValue.getDeviceId());
        if (Objects.isNull(fencingToken)) {
            log.error("Reject point value without active device lease, deviceId={}, pointId={}, node={}",
                    pointValue.getDeviceId(), pointValue.getPointId(), driverProperties.getNode());
            return false;
        }
        pointValue.setMessageId(UUID.randomUUID().toString());
        pointValue.setSchemaVersion(POINT_VALUE_SCHEMA_VERSION);
        pointValue.setDriverNode(driverProperties.getNode());
        pointValue.setSequence(pointValueSequence.incrementAndGet());
        pointValue.setFencingToken(fencingToken);
        if (log.isDebugEnabled()) {
            log.debug("Point value staged, messageId={}, tenantId={}, driverId={}, deviceId={}, pointId={}, sequence={}",
                    pointValue.getMessageId(), pointValue.getTenantId(), pointValue.getDriverId(),
                    pointValue.getDeviceId(), pointValue.getPointId(), pointValue.getSequence());
        }
        return true;
    }

    /**
     * Persist the supplied point values in one outbox transaction, then publish them.
     *
     * @param entityDTOList point value payloads, may be null
     */
    @Override
    public void pointValueSender(List<PointValue> entityDTOList) {
        if (Objects.isNull(entityDTOList) || entityDTOList.isEmpty()) {
            return;
        }
        DriverBO driver = driverMetadata.getDriver();
        if (Objects.isNull(driver)) {
            log.error("Reject point-value batch before driver registration, size={}", entityDTOList.size());
            return;
        }
        List<PointValue> pointValues = new ArrayList<>(entityDTOList.size());
        for (PointValue pointValue : entityDTOList) {
            if (Objects.nonNull(pointValue) && stampPointValue(pointValue, driver)) {
                pointValues.add(pointValue);
            }
        }
        if (!pointValues.isEmpty()) {
            bufferService.publishBatch(pointValues, driverProperties.getService());
        }
    }

    /**
     * Publish the result of a point-level command invocation.
     *
     * @param resultDTO point command result payload
     */
    @Override
    public void pointCommandResultSender(PointCommandResultDTO resultDTO) {
        if (Objects.isNull(resultDTO)) {
            return;
        }
        sendConfirmed(MqTopic.POINT_COMMAND_RESULT, driverProperties.getService(), resultDTO);
    }

    /**
     * Publish the result of a device-level command invocation.
     *
     * @param resultDTO command call result payload
     */
    @Override
    public void commandResultSender(CommandCallResultDTO resultDTO) {
        if (Objects.isNull(resultDTO)) {
            return;
        }
        sendConfirmed(MqTopic.COMMAND_RESULT, driverProperties.getService(), resultDTO);
    }

    /**
     * Publish a driver/device event report to the event exchange.
     *
     * @param entityDTO event report payload
     */
    @Override
    public void eventReportSender(EventReportDTO entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return;
        }
        messageSender.send(MqMessage.of(MqTopic.EVENT, driverProperties.getService(), entityDTO));
    }

    /**
     * Assemble and publish a device state, filling driver/tenant context from the
     * registered driver metadata (warn-and-skip when the driver is not registered yet).
     *
     * @param deviceId         target device
     * @param status           device status to report
     * @param timeout          lease timeout value
     * @param timeoutUnit      lease timeout unit
     * @param stateDescription optional structured description, may be null
     */
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
            log.warn("Device state publish degraded, reason=driverNotRegistered, deviceId={}", deviceId);
        }
        log.info("Device state published, deviceId={}, status={}", deviceId, status.getCode());
        deviceStateSender(deviceState);
    }

    private void sendConfirmed(MqTopic topic, String partitionKey, Object payload) {
        messageSender.sendConfirmed(MqMessage.of(topic, partitionKey, payload), Duration.ofSeconds(5));
    }

}
