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

package io.github.pnoker.center.manager.receiver.rabbit;

import cn.hutool.core.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.center.manager.biz.DriverSyncService;
import io.github.pnoker.common.entity.dto.DriverRegisterDTO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 驱动注册消息接收
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverRegisterReceiver {

    @Resource
    private DriverSyncService driverSyncService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverRegisterQueue.name}")
    public void driverRegisterReceive(Channel channel, Message message, DriverRegisterDTO entityDTO) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (ObjectUtil.isNull(entityDTO)) {
                log.error("跳过: 接收到的驱动注册信息无效");
                return;
            }
            log.debug("接收到驱动注册信息: {}", JsonUtil.toPrettyJsonString(entityDTO));
            driverSyncService.up(entityDTO);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
