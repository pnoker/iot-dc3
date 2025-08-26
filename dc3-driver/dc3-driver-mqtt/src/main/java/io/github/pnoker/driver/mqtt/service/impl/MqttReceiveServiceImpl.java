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

package io.github.pnoker.driver.mqtt.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class MqttReceiveServiceImpl implements MqttReceiveService {

    @Resource
    private DriverSenderService driverSenderService;

    @Override
    public void receiveValue(MqttMessage mqttMessage) {
        // do something to process your mqtt messages
        log.info(JsonUtil.toJsonString(mqttMessage));
        PointValue pointValue = JsonUtil.parseObject(mqttMessage.getPayload(), PointValue.class);
        pointValue.setCreateTime(LocalDateTimeUtil.now());
        driverSenderService.pointValueSender(pointValue);
    }

    @Override
    public void receiveValues(List<MqttMessage> mqttMessageList) {
        // do something to process your mqtt messages
        log.info(JsonUtil.toJsonString(mqttMessageList));
        List<PointValue> pointValues = mqttMessageList.stream()
                .map(mqttMessage -> {
                    PointValue pointValue = JsonUtil.parseObject(mqttMessage.getPayload(), PointValue.class);
                    pointValue.setCreateTime(LocalDateTimeUtil.now());
                    return pointValue;
                }).toList();
        driverSenderService.pointValueSender(pointValues);
    }
}
