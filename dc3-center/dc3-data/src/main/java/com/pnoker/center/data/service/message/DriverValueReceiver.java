/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.center.data.service.message;

import com.pnoker.center.data.service.PointValueService;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import javax.annotation.Resource;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 */
@Slf4j
@EnableBinding(TopicInput.class)
public class DriverValueReceiver {
    @Resource
    private PointValueService pointValueService;

    @StreamListener(Common.Topic.DRIVER_VALUE_TOPIC)
    public void driverReceive(PointValue pointValue) {
        pointValueService.add(pointValue);
    }
}
