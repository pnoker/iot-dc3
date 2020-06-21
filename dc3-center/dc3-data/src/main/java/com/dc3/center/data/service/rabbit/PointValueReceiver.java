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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 */
@Slf4j
@Component
@RabbitListener(queues = Common.Rabbit.POINT_VALUE_QUEUE)
public class PointValueReceiver {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private PointValueService pointValueService;

    @RabbitHandler
    public void pointValueReceive(PointValue pointValue) {
        if (null == pointValue || null == pointValue.getDeviceId() || null == pointValue.getPointId()) {
            log.error("Invalid data: {}", pointValue);
            return;
        }

        /*
        Convention:
        PointId = 0 indicates device status
        PointId > 0 indicates device point data
         */
        if (pointValue.getPointId().equals(0L)) {
            log.info("Received device({}) status({})", pointValue.getDeviceId(), pointValue.getRawValue());
            // Save device status to redis, 15 minutes
            redisUtil.setKey(
                    Common.Cache.DEVICE_STATUS_KEY_PREFIX + pointValue.getDeviceId(),
                    pointValue.getRawValue(),
                    15,
                    TimeUnit.MINUTES);
        } else {
            // LinkedBlockingQueue ThreadPoolExecutor
            threadPoolExecutor.execute(() -> {
                log.debug("Received data: {}", pointValue);
                // Save device point data to redis, 15 minutes
                redisUtil.setKey(
                        Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + "_" + pointValue.getPointId(),
                        pointValue.getValue(),
                        15,
                        TimeUnit.MINUTES);
                // Push device point data to MQ
                pointValueService.add(pointValue);
            });
        }
    }
}
