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

package com.dc3.driver.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.CustomDriverService;
import com.dc3.common.sdk.service.rabbit.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomDriverServiceImpl implements CustomDriverService {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverContext driverContext;

    @Override
    public void initial() {
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        log.debug("Virtual Driver Read, device: {}, point: {}", JSON.toJSONString(device), JSON.toJSONString(point));
        return String.valueOf(RandomUtil.randomDouble(100));
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        return false;
    }

    @Override
    public void schedule() {
        driverContext.getDeviceMap().keySet().forEach(id -> driverService.deviceStatusSender(id, Common.Device.ONLINE));
    }

}
