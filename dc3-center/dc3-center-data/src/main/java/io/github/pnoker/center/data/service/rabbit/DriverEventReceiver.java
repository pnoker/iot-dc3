/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service.rabbit;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.center.data.service.DriverEventService;
import io.github.pnoker.common.dto.DriverEventDTO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 接收驱动发送过来的驱动事件数据
 * 其中包括：驱动心跳事件、在线、离线、故障等其他事件
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverEventReceiver {

    @Resource
    private DriverEventService driverEventService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEventDTO entityDTO) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.debug("Receive driver event: {}", JsonUtil.toPrettyJsonString(entityDTO));
            if (ObjectUtil.isNull(entityDTO)
                    || ObjectUtil.isNull(entityDTO.getType())
                    || CharSequenceUtil.isEmpty(entityDTO.getContent())) {
                log.error("Invalid driver event: {}", entityDTO);
                return;
            }

            switch (entityDTO.getType()) {
                case HEARTBEAT:
                    driverEventService.heartbeatEvent(entityDTO);
                    break;
                case ALARM:
                    //statusEvent(entityDTO);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
