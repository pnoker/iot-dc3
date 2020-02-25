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

package com.pnoker.common.sdk.service.message;

import com.pnoker.common.bean.driver.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@EnableBinding(TopicOutput.class)
public class DriverMessageSender {

    @Resource
    private TopicOutput topicOutput;

    public void driverSender(PointValue pointValue) {
        topicOutput.driverOutput().send(
                MessageBuilder.withPayload(pointValue).build()
        );
    }
}
