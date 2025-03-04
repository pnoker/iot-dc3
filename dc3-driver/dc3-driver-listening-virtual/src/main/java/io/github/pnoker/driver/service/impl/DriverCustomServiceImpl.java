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

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.driver.service.netty.tcp.NettyTcpServer;
import io.github.pnoker.driver.service.netty.udp.NettyUdpServer;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 驱动自定义服务实现类
 *
 * @author pnoker
 * @version 2025.2.1
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    DriverMetadata driverMetadata;
    @Value("${driver.custom.tcp.port}")
    private Integer tcpPort;
    @Value("${driver.custom.udp.port}")
    private Integer udpPort;
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
         * 驱动初始化逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 驱动启动时会自动执行该方法，您可以在此处执行特定的初始化操作。
         *
         * 例如: 启动 TCP 和 UDP 监听服务，分别监听指定的端口，用于接收外部数据。
         */
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
    public void schedule() {
        /*
         * 设备状态上传逻辑
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         * 设备状态的上传可以根据具体需求灵活实现，以下是一些常见的实现方式：
         * - 在 `read` 方法中根据读取的数据判断设备状态；
         * - 在自定义的定时任务中定期检查设备状态；
         * - 根据特定的业务逻辑或事件触发设备状态的判断。
         *
         * 最终通过 {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} 接口将设备状态提交给 SDK 管理。
         * 设备状态枚举 {@link DeviceStatusEnum} 包含以下状态：
         * - ONLINE: 设备在线
         * - OFFLINE: 设备离线
         * - MAINTAIN: 设备维护中
         * - FAULT: 设备故障
         *
         * 在以下示例中，所有设备的状态被设置为 {@link DeviceStatusEnum#ONLINE}，并设置状态的有效期为 25 {@link TimeUnit#SECONDS}。
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * 接收驱动、设备、位号元数据的新增、更新、删除事件。
         *
         * 元数据类型: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * 元数据操作类型: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * 提示: 此处逻辑仅供参考，请务必结合实际应用场景进行修改。
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * 重要提示: 以下逻辑仅供参考，请根据实际应用场景进行调整。
         *
         * 由于 Listening Virtual 的数据来源是被动接收的，因此无需在此实现 `read` 方法。
         * 数据接收处理逻辑已在以下类中实现：
         * - {@link io.github.pnoker.driver.service.netty.tcp.NettyTcpServerHandler#channelRead}
         * - {@link io.github.pnoker.driver.service.netty.udp.NettyUdpServerHandler#channelRead0}
         */
        return null;
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * 重要提示: 以下逻辑仅供参考，请根据实际应用场景进行调整。
         *
         * 该方法的实现依赖于 Netty TCP 服务器的设备通道映射表。如果设备对应的通道存在，
         * 则将写入的值转换为字节并发送到该通道。
         *
         * 返回值始终为 true，表示写入操作已成功处理。
         */
        Long deviceId = device.getId();
        Channel channel = NettyTcpServer.deviceChannelMap.get(deviceId);
        if (Objects.nonNull(channel)) {
            channel.writeAndFlush(DecodeUtil.stringToByte(wValue.getValue()));
        }
        return true;
    }

}
