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

package io.github.pnoker.center.data.receiver.rabbit;

import cn.hutool.core.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.center.data.biz.PointValueService;
import io.github.pnoker.center.data.job.PointValueJob;
import io.github.pnoker.center.data.mqtt.service.MqttSendService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.property.MqttProperties;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接收驱动发送过来的数据
 * <p>
 * 200万条SinglePointValue会产生：60M的索引数据以及400M的数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class PointValueReceiver {

    @Value("${data.point.batch.speed}")
    private Integer batchSpeed;

    @Resource
    private MqttProperties mqttProperties;

    @Resource
    private PointValueService pointValueService;
    @Resource
    private MqttSendService mqttSendService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{pointValueQueue.name}", containerFactory = "")
    public void pointValueReceive(Channel channel, Message message, PointValueBO pointValueBO) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (ObjectUtil.isNull(pointValueBO) || ObjectUtil.isNull(pointValueBO.getDeviceId())) {
                log.error("Invalid point value: {}", pointValueBO);
                return;
            }
            PointValueJob.valueCount.getAndIncrement();
            log.debug("Point value, From: {}, Received: {}", message.getMessageProperties().getReceivedRoutingKey(), pointValueBO);

            // Judge whether to process data in batch according to the data transmission speed
            if (PointValueJob.valueSpeed.get() < batchSpeed) {
                threadPoolExecutor.execute(() ->
                        // Save point value to Redis & MongoDB
                        pointValueService.save(pointValueBO)
                );
            } else {
                // Save point value to schedule
                PointValueJob.valueLock.writeLock().lock();
                PointValueJob.addPointValues(pointValueBO);
                PointValueJob.valueLock.writeLock().unlock();
            }

            // Forward data to MQTT
            String topic = mqttProperties.getDefaultSendTopic().getName() + "/" + pointValueBO.getDeviceId();
            log.debug("Forward dat to device: {}:{}", pointValueBO.getDeviceId(), topic);
            mqttSendService.sendToMqtt(topic, JsonUtil.toJsonString(pointValueBO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
