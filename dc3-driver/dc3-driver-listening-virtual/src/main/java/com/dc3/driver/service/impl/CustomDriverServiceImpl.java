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
import com.dc3.common.sdk.service.DriverService;
import com.dc3.driver.service.netty.tcp.NettyTcpServer;
import com.dc3.driver.service.netty.udp.NettyUdpServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomDriverServiceImpl implements CustomDriverService {

    @Value("${driver.custom.tcp.port}")
    private Integer tcpPort;
    @Value("${driver.custom.udp.port}")
    private Integer udpPort;

    @Resource
    private NettyTcpServer nettyTcpServer;
    @Resource
    private NettyUdpServer nettyUdpServer;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void initial() {
        threadPoolExecutor.execute(() -> {
            log.debug("Virtual Listening Driver Starting(TCP::{}) incoming data listener", tcpPort);
            nettyTcpServer.start(tcpPort);
        });
        threadPoolExecutor.execute(() -> {
            log.debug("Virtual Listening Driver Starting(UDP::{}) incoming data listener", udpPort);
            nettyUdpServer.start(udpPort);
        });
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) throws Exception {
        return "nil";
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) throws Exception {
        Long deviceId = device.getId();

        // TODO 获取设备的Channel，并向下发送数据
        Channel channel = NettyTcpServer.deviceChannelMap.get(deviceId);
        if (null != channel) {
            channel.writeAndFlush(value.getValue().getBytes(StandardCharsets.UTF_8));
        }
        return true;
    }

    @Override
    public void schedule() {

        /*
        TODO:设备状态
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，也可以在read中实现设备状态的设置；
        你可以通过某种判断机制确定设备的状态，然后通过driverService.deviceStatusSender(deviceId,DeviceStatus)接口将设备状态交给SDK管理。

        设备状态（DeviceStatus）如下：
        ONLINE:在线
        OFFLINE:离线
        MAINTAIN:维护
        FAULT:故障
         */
        driverContext.getDeviceMap().keySet().forEach(id -> driverService.deviceStatusSender(id, Common.Device.Status.ONLINE));
    }

}
