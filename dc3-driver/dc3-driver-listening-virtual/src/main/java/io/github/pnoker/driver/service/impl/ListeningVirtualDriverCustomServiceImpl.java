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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of custom driver service for the listening virtual driver.
 * <p>
 * This service handles driver initialization, scheduling, metadata events, and read/write
 * operations for devices that communicate via TCP/UDP.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListeningVirtualDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;

    @Value("${dc3.driver.custom.tcp.port}")
    private Integer tcpPort;

    @Value("${dc3.driver.custom.udp.port}")
    private Integer udpPort;

    private final DriverSenderService driverSenderService;

    private final NettyTcpServer nettyTcpServer;

    private final NettyUdpServer nettyUdpServer;

    private final ThreadPoolExecutor threadPoolExecutor;

    /**
     * Initializes the driver by starting TCP and UDP listening services.
     * <p>
     * This method is automatically called when the driver starts. It launches separate
     * threads to listen for incoming TCP and UDP connections on the configured ports.
     * </p>
     */
    @Override
    public void initial() {
        threadPoolExecutor.execute(() -> {
            log.info("Driver listener starting, protocol=tcp, port={}", tcpPort);
            nettyTcpServer.start(tcpPort);
        });
        threadPoolExecutor.execute(() -> {
            log.info("Driver listener starting, protocol=udp, port={}", udpPort);
            nettyUdpServer.start(udpPort);
        });
    }

    /**
     * Scheduled task to report device status.
     * <p>
     * Sets all devices to ONLINE status with a validity period of 25 seconds. This method
     * is called periodically by the driver framework.
     * </p>
     */
    @Override
    public void schedule() {
        driverMetadata.getDeviceIds()
                .forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    /**
     * Handles metadata events for drivers, devices, and points.
     * <p>
     * Processes addition, update, and deletion events for metadata. Currently, logs
     * device and point metadata events for monitoring purposes.
     * </p>
     *
     * @param metadataEvent The metadata event containing type and operation details
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=listeningVirtual, metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=listeningVirtual, metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    /**
     * Reads data from a device point.
     * <p>
     * Since this driver passively receives data via TCP/UDP, this method returns null.
     * Actual data reading is handled by the TCP and UDP server handlers in their
     * respective channelRead methods.
     * </p>
     *
     * @param driverConfig Driver configuration attributes
     * @param pointConfig  Point configuration attributes
     * @param device       The device to read from
     * @param point        The point to read
     * @return null as data is received passively
     */
    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        return null;
    }

    /**
     * Writes data to a device point.
     * <p>
     * Writes the specified value to the device by sending it through the Netty TCP
     * channel associated with the device. If the device channel exists in the
     * device-channel mapping, the value is converted to bytes and sent to the device.
     * </p>
     *
     * @param driverConfig    Driver configuration attributes
     * @param pointConfig     Point configuration attributes
     * @param device          The device to write to
     * @param point           The point to write
     * @param writePointValue The value to write
     * @return true if the write operation was processed
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        Long deviceId = device.getId();
        Channel channel = NettyTcpServer.getDeviceChannel(deviceId);
        if (Objects.nonNull(channel)) {
            log.debug("Driver point write requested, protocol=tcp, deviceId={}, pointId={}, valueLength={}", deviceId,
                    point.getId(), Objects.toString(writePointValue.getValue(), "").length());
            channel.writeAndFlush(DecodeUtil.stringToByte(writePointValue.getValue()));
        } else {
            log.warn("Driver point write skipped, protocol=tcp, deviceId={}, pointId={}, reason=channelMissing",
                    deviceId, point.getId());
        }
        return true;
    }

}
