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

package io.github.pnoker.driver.service.impl;

import com.mchange.v2.lang.StringUtils;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.entity.bo.AttributeBO;
import io.github.pnoker.common.entity.dto.DeviceDTO;
import io.github.pnoker.common.entity.dto.PointDTO;
import io.github.pnoker.common.utils.AttributeUtil;
import io.github.pnoker.driver.server.Lwm2mServer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;


/**
 * @author xwh1998
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private Lwm2mServer lwm2mServer;


    @Override
    public void initial() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        你可以在此处执行一些特定的初始化逻辑, 驱动在启动的时候会自动执行该方法。
        */
        lwm2mServer.startServer();
    }

    @Override
    public void schedule() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        上传设备状态, 可自行灵活拓展, 不一定非要在schedule()接口中实现, 你可以: 
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 根据某种判断机制实现设备状态的判断。

        最后根据 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理, 其中设备状态（StatusEnum）:
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
    }

    /**
     * 读取值 or 订阅
     *
     * @param driverConfig Driver Attribute Config
     * @param pointConfig  Point Attribute Config
     * @param device       Device
     * @param point        Point
     * @return
     */
    @Override
    public String read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, PointDTO point) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!

        可以主动读取,也可以订阅资源
         */
        AttributeBO messageUpAttribute = pointConfig.get("messageUp");
        return lwm2mServer.readValueByPath(String.valueOf(device.getId()), AttributeUtil.getAttributeValue(messageUpAttribute, String.class));
    }

    /**
     * 写入值 or 执行函数
     * <p>
     * 注意配置只写位号时,只可以配消息下行或命令下行其中一个
     *
     * @param driverConfig Driver Attribute Config
     * @param pointConfig  Point Attribute Config
     * @param device       Device
     * @param value        Value Attribute Config
     * @return
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, AttributeBO value) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        AttributeBO execDownAttribute = pointConfig.get("execDown");
        String execDownValue = AttributeUtil.getAttributeValue(execDownAttribute, String.class);
        if (StringUtils.nonEmptyString(execDownValue)) {
            //执行函数
            return lwm2mServer.execute(String.valueOf(device.getId()), execDownValue, value.getValue());
        }
        AttributeBO messageDownAttribute = pointConfig.get("messageDown");
        String messageDownValue = AttributeUtil.getAttributeValue(messageDownAttribute, String.class);
        return lwm2mServer.writeValueByPath(String.valueOf(device.getId()), messageDownValue, value.getValue(), false);
    }
}
