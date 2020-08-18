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
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.constant.Common;
import com.dc3.common.utils.RedisUtil;
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
 *
 * @author pnoker
 */
@Slf4j
@Component
public class MultiplePointValueReceiver {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private PointValueService pointValueService;

    @RabbitHandler
    @RabbitListener(queues = "#{multiPointValueQueue.name}")
    public void pointValueReceive(Channel channel, Message message, PointValue pointValue) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.debug("Multi point data from {}", message.getMessageProperties().getReceivedRoutingKey());

            if (null == pointValue || null == pointValue.getDeviceId() || null == pointValue.getChildren()) {
                log.error("Invalid multi point data: {}", pointValue);
                return;
            }

            threadPoolExecutor.execute(() -> {
                log.debug("Received multi point data: {}", pointValue);
                // Save device point data to redis, 15 minutes
                redisUtil.setKey(
                        Common.Cache.REAL_TIME_VALUES_KEY_PREFIX + pointValue.getDeviceId(),
                        pointValue.getChildren(),
                        pointValue.getTimeOut(),
                        pointValue.getTimeUnit()
                );
                // Insert device point data to MongoDB
                // TODO 可根据项目并发情况实现一个定时和批量入库逻辑
                pointValueService.addPointValue(pointValue.setMulti(true));
            });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
