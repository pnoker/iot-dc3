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

import com.dc3.common.constant.Common;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.CustomDriverService;
import com.dc3.common.sdk.service.rabbit.DriverService;
import com.dc3.driver.service.netty.NettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomDriverServiceImpl implements CustomDriverService {

    @Value("${driver.custom.socket.port}")
    private Integer port;

    @Resource
    private NettyServer nettyServer;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void initial() {
        threadPoolExecutor.execute(() -> {
            log.debug("Virtual Listening Driver Starting(::{}) incoming data listener", port);
            nettyServer.start(port);
        });
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        return "nil";
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        return false;
    }

    @Override
    public void schedule() {
        //TODO 上传设备状态，可自行灵活拓展
        driverContext.getDeviceMap().keySet().forEach(id -> driverService.deviceStatusSender(id, Common.Device.ONLINE));
    }

}
