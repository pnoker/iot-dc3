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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.driver.service.MqttSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Custom driver service implementation for the MQTT driver.
 * <p>
 * This service provides MQTT-specific device communication capabilities. Since MQTT is a
 * publish-subscribe protocol, data is passively received through subscriptions rather
 * than actively polled. The read method returns null as data is received asynchronously
 * through the MQTT receive handler.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final MqttSendService mqttSendService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    /**
     * Initializes the MQTT driver.
     * <p>
     * This method is called when the driver starts. Override this method to implement
     * custom initialization logic specific to your MQTT devices and subscriptions.
     * </p>
     */
    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. This method is automatically executed when the
         * driver starts, and you can perform specific initialization operations here.
         *
         */
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    /**
     * Handles metadata change events for drivers, devices, and points.
     * <p>
     * This method is called when metadata is created, updated, or deleted. Override this
     * method to implement custom event handling logic such as managing MQTT topic
     * subscriptions.
     * </p>
     *
     * @param metadataEvent the metadata event containing type, operation, and ID
     *                      information
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * Receive metadata events for driver, device, and point creation, update, and
         * deletion.
         *
         * Metadata type: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT) Metadata
         * operation type: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario.
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Driver metadata event received, protocol=" + driverCode + ", metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    /**
     * Reads data from an MQTT device point.
     * <p>
     * Since MQTT uses a publish-subscribe model, data is passively received through
     * subscriptions rather than actively polled. This method returns null as the actual
     * data processing is handled by
     * {@link io.github.pnoker.common.mqtt.handler.MqttReceiveHandler#handlerValue}.
     * </p>
     *
     * @param driverConfig driver configuration attributes
     * @param pointConfig  point configuration attributes
     * @param device       the device to read from
     * @param point        the point to read
     * @return null (data is received asynchronously)
     */
    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        /*
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario.
         *
         * Since MQTT data is passively received, there is no need to implement the `read`
         * method here. The processing logic for receiving data has been implemented in
         * {@link io.github.pnoker.common.mqtt.handler.MqttReceiveHandler#handlerValue}.
         */
        return null;
    }

    /**
     * Writes data to an MQTT device point.
     * <p>
     * This method publishes messages to MQTT topics for device control. It retrieves the
     * command topic from point configuration and optionally uses a configured QoS level.
     * If QoS configuration fails, it falls back to the default QoS.
     * </p>
     *
     * @param driverConfig driver configuration attributes
     * @param pointConfig  point configuration attributes (must contain "commandTopic",
     *                     optionally "commandQos")
     * @param device       the device to write to
     * @param point        the point to write
     * @param values       the value containing the data to write
     * @return true if the write operation succeeded, false otherwise
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue values) {
        /*
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario.
         *
         * This method is used to write data to an MQTT topic. First, retrieve the command
         * topic `commandTopic` and the value to be sent `value` from `pointConfig`. If
         * the configuration specifies a QoS level `commandQos`, attempt to send the
         * message with the specified QoS; if not specified or an exception occurs, send
         * the message with the default QoS. Finally, return `true` to indicate a
         * successful write operation.
         */
        String commandTopic = pointConfig.get("commandTopic").getValue(String.class);
        String value = values.getValue();
        log.debug("Driver point write requested, protocol=" + driverCode + ", deviceId={}, pointId={}, topic={}, valueLength={}",
                device.getId(), point.getId(), commandTopic, Objects.toString(value, "").length());
        try {
            int commandQos = pointConfig.get("commandQos").getValue(Integer.class);
            mqttSendService.sendToMqtt(commandTopic, commandQos, value);
        } catch (Exception e) {
            log.warn("MQTT command QoS unavailable, fallback to default, deviceId={}, pointId={}, topic={}",
                    device.getId(), point.getId(), commandTopic, e);
            mqttSendService.sendToMqtt(commandTopic, value);
        }
        return true;
    }

}
