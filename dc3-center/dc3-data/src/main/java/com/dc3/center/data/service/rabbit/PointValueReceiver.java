/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service.rabbit;

import com.dc3.center.data.service.DataCustomService;
import com.dc3.center.data.service.PointValueService;
import com.dc3.center.data.service.job.PointValueScheduleJob;
import com.dc3.common.model.PointValue;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接收驱动发送过来的数据
 * <p>
 * 200万条SinglePointValue会产生：60M的索引数据以及400M的数据
 *
 * @author pnoker
 */
@Slf4j
@Component
public class PointValueReceiver {

    @Value("${data.point.batch.speed}")
    private Integer batchSpeed;

    @Resource
    private PointValueService pointValueService;
    @Resource
    private DataCustomService dataCustomService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{pointValueQueue.name}")
    public void pointValueReceive(Channel channel, Message message, PointValue pointValue) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == pointValue || null == pointValue.getDeviceId()) {
                log.error("Invalid point value: {}", pointValue);
                return;
            }
            PointValueScheduleJob.valueCount.getAndIncrement();
            log.debug("Point value, From: {}, Received: {}", message.getMessageProperties().getReceivedRoutingKey(), pointValue);

            // pre handle
            dataCustomService.preHandle(pointValue);

            // Judge whether to process data in batch according to the data transmission speed
            if (PointValueScheduleJob.valueSpeed.get() < batchSpeed) {
                threadPoolExecutor.execute(() -> {
                    // Save point value to Redis & MongoDB
                    pointValueService.savePointValue(pointValue);
                });
            } else {
                // Save point value to schedule
                PointValueScheduleJob.valueLock.writeLock().lock();
                PointValueScheduleJob.pointValues.add(pointValue);
                PointValueScheduleJob.valueLock.writeLock().unlock();
            }

            // after handle
            dataCustomService.afterHandle(pointValue);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
