/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.service.DriverCustomService;
import io.github.pnoker.driver.sdk.service.DriverSenderService;
import io.github.pnoker.driver.service.netty.tcp.NettyTcpServer;
import io.github.pnoker.driver.service.netty.udp.NettyUdpServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Value("${driver.custom.tcp.port}")
    private Integer tcpPort;
    @Value("${driver.custom.udp.port}")
    private Integer udpPort;

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverSenderService driverSenderService;
    @Resource
    private NettyTcpServer nettyTcpServer;
    @Resource
    private NettyUdpServer nettyUdpServer;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void initial() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        你可以在此处执行一些特定的初始化逻辑，驱动在启动的时候会自动执行该方法。
        */
        threadPoolExecutor.execute(() -> {
            log.debug("Dtu YeeCom Driver Starting(TCP::{}) incoming data listener", tcpPort);
            nettyTcpServer.start(tcpPort);
        });
        threadPoolExecutor.execute(() -> {
            log.debug("Dtu YeeCom Driver Starting(UDP::{}) incoming data listener", udpPort);
            nettyUdpServer.start(udpPort);
        });
    }

    @Override
    public void schedule() {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
        !!!
        上传设备状态，可自行灵活拓展，不一定非要在schedule()接口中实现，你可以：
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 通过某种判断机制实现设备状态的判断。

        最后通过 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理，其中设备状态（StatusEnum）：
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        driverContext.getDriverMetadata().getDeviceMap().keySet().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE));
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!

        因为 Listening Virtual 的数据来源是被动接收的，所以无需实现该 Read 方法
        接收数据处理函数在
        - io.github.pnoker.driver.service.netty.tcp.NettyTcpServerHandler.channelRead
        - io.github.pnoker.driver.service.netty.udp.NettyUdpServerHandler.channelRead0
        */
        return DefaultConstant.DEFAULT_VALUE;
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {
        /*
        !!! 提示：此处逻辑仅供参考，请务必结合实际应用场景。!!!
         */
        String deviceId = device.getId();
        Channel channel = NettyTcpServer.deviceChannelMap.get(deviceId);
        if (ObjectUtil.isNotNull(channel)) {
            channel.writeAndFlush(DecodeUtil.stringToByte(value.getValue()));
        }
        return true;
    }

}
