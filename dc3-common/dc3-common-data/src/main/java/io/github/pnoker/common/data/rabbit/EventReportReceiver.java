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

import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for event reports published by protocol drivers.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventReportReceiver {

    private final EventHistoryService eventHistoryService;

    /**
     * Consume an event report message and forward it to the event history service.
     *
     * @param channel   the RabbitMQ channel for manual ack
     * @param message   the raw message carrying the delivery tag
     * @param entityDTO the deserialized event report
     */
    @Dc3Listener(topic = MqTopic.EVENT)
    public void onEventReport(MqReceived<EventReportDTO> message, Acknowledgment ack) {
        EventReportDTO entityDTO = message.payload();
        try {
            log.debug("Event report received, recordId={}, deviceId={}, eventId={}",
                    Objects.isNull(entityDTO) ? null : entityDTO.recordId(),
                    Objects.isNull(entityDTO) ? null : entityDTO.deviceId(),
                    Objects.isNull(entityDTO) ? null : entityDTO.eventId());
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.recordId())
                    || Objects.isNull(entityDTO.deviceId()) || Objects.isNull(entityDTO.eventId())) {
                log.warn("Invalid event report, some required fields are null, recordId={}, deviceId={}, eventId={}",
                        Objects.isNull(entityDTO) ? null : entityDTO.recordId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.deviceId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.eventId());
                ack.reject(false);
                return;
            }
            eventHistoryService.report(entityDTO);
            ack.ack();
        } catch (Exception e) {
            log.error("Event report consume failed.", e);
            ack.reject(true);
        }
    }

}
