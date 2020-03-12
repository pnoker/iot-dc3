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

package com.github.pnoker.common.sdk.service.rabbit;

import cn.hutool.core.convert.Convert;
import com.github.pnoker.common.sdk.bean.DriverProperty;
import com.github.pnoker.common.bean.driver.PointValue;
import com.github.pnoker.common.constant.Common;
import com.github.pnoker.common.exception.ServiceException;
import com.github.pnoker.common.model.Point;
import com.github.pnoker.common.sdk.bean.DriverContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class PointValueService {
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private DriverContext driverContext;
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 将位号原始值进行处理和转换
     *
     * @param deviceId
     * @param pointId
     * @param rawValue
     * @return
     */
    public PointValue convertValue(Long deviceId, Long pointId, String rawValue) {
        return new PointValue(deviceId, pointId, rawValue, processValue(rawValue, driverContext.getDevicePoint(deviceId, pointId)));
    }

    /**
     * 发送位号值到消息组件
     *
     * @param pointValue
     */
    public void pointValueSender(PointValue pointValue) {
        log.debug("send point value,{}", pointValue);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE, "key." + driverProperty.getName(), pointValue);
    }

    /**
     * 批量发送位号值到消息组件
     *
     * @param pointValues
     */
    public void pointValueSender(List<PointValue> pointValues) {
        for (PointValue pointValue : pointValues) {
            pointValueSender(pointValue);
        }
    }

    /**
     * 处理数值
     *
     * @param value
     * @param point point.type : string/int/double/float/long/boolean
     * @return
     */
    private String processValue(String value, Point point) {
        value = value.trim();
        switch (point.getType()) {
            case Common.ValueType.STRING:
                break;
            case Common.ValueType.INT:
            case Common.ValueType.LONG:
                try {
                    value = String.format("%.0f",
                            (Convert.convert(Double.class, value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            case Common.ValueType.DOUBLE:
            case Common.ValueType.FLOAT:
                try {
                    value = String.format(point.getFormat(),
                            (Convert.convert(Double.class, value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            case Common.ValueType.BOOLEAN:
                try {
                    value = String.valueOf(Boolean.parseBoolean(value));
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            default:
                throw new ServiceException("invalid device point value type");
        }
        return value;
    }
}
