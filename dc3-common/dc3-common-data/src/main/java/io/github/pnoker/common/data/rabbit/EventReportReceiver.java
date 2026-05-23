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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.EventReportService;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for event reports published by protocol drivers.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventReportReceiver {

    private final EventReportService eventReportService;

    @RabbitHandler
    @RabbitListener(queues = "#{eventReportQueue.name}")
    public void onEventReport(Channel channel, Message message, EventReportDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Receive event report: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.recordId())
                    || Objects.isNull(entityDTO.deviceId()) || Objects.isNull(entityDTO.eventId())) {
                log.error("Invalid event report: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            eventReportService.report(entityDTO);
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Event report consume failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
