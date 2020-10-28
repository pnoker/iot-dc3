/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service.rabbit;

import com.dc3.center.data.service.PointValueService;
import com.dc3.center.data.service.job.PointValueScheduleJob;
import com.dc3.common.bean.driver.PointValue;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    @Resource
    private PointValueService pointValueService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{pointValueQueue.name}")
    public void pointValueReceive(Channel channel, Message message, PointValue pointValue) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == pointValue || null == pointValue.getDeviceId()) {
                log.error("Invalid point data: {}", pointValue);
                return;
            }
            PointValueScheduleJob.valueCount.getAndIncrement();
            log.debug("Point value, From: {}, Received: {}", message.getMessageProperties().getReceivedRoutingKey(), pointValue);

            if (PointValueScheduleJob.valueSpeed.get() < 100) {
                threadPoolExecutor.execute(() -> {
                    // Save point value to Redis & MongoDB
                    pointValueService.addPointValue(pointValue);
                });
            } else {
                // Save point value to schedule
                PointValueScheduleJob.valueLock.writeLock().lock();
                PointValueScheduleJob.pointValues.add(pointValue);
                PointValueScheduleJob.valueLock.writeLock().unlock();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
