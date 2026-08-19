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

package io.github.pnoker.common.data.rabbit;

import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for driver alarm events.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverAlarmReceiver {

    private final DriverAlarmService driverAlarmService;

    /**
     * Consume a driver alarm message and forward it to the alarm service for processing.
     *
     * @param channel   the RabbitMQ channel for manual ack
     * @param message   the raw message carrying the delivery tag
     * @param entityDTO the deserialized driver alarm
     */
    @Dc3Listener(topic = MqTopic.ALARM, keyPattern = "driver.*")
    public void driverAlarmReceive(MqReceived<DriverAlarmDTO> message, Acknowledgment ack) {
        DriverAlarmDTO entityDTO = message.payload();
        try {
            log.debug("Driver alarm received, tenantId={}, driverId={}",
                    Objects.isNull(entityDTO) ? null : entityDTO.getTenantId(),
                    Objects.isNull(entityDTO) ? null : entityDTO.getDriverId());
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId())) {
                log.warn("Invalid driver alarm, driverId is null, driverId={}",
                        Objects.isNull(entityDTO) ? null : entityDTO.getDriverId());
                ack.reject(false);
                return;
            }
            driverAlarmService.alarm(entityDTO);
            ack.ack();
        } catch (Exception e) {
            log.error("Driver alarm consume failed.", e);
            ack.reject(true);
        }
    }

}
