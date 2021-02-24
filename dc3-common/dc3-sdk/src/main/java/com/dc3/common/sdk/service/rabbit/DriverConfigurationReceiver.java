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

package com.dc3.common.sdk.service.rabbit;

import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson.JSON;
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.bean.driver.DriverMetadata;
import com.dc3.common.constant.Common;
import com.dc3.common.model.*;
import com.dc3.common.sdk.service.DriverMetadataService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverConfigurationReceiver {

    @Resource
    private DriverMetadataService driverMetadataService;

    /**
     * 配置 driver profile，增删改
     *
     * @param command 操作类型
     * @param profile Profile
     */
    private void configurationProfile(String command, Profile profile) {
        if (Common.Driver.Profile.ADD.equals(command) || Common.Driver.Profile.UPDATE.equals(command)) {
            driverMetadataService.upsertProfile(profile);
        }
        if (Common.Driver.Profile.DELETE.equals(command)) {
            driverMetadataService.deleteProfile(profile.getId());
        }
    }

    /**
     * 配置 driver device，增删改
     *
     * @param command 操作类型
     * @param device  Device
     */
    private void configurationDevice(String command, Device device) {
        if (Common.Driver.Device.ADD.equals(command) || Common.Driver.Device.UPDATE.equals(command)) {
            driverMetadataService.upsertDevice(device);
        }
        if (Common.Driver.Device.DELETE.equals(command)) {
            driverMetadataService.deleteDevice(device.getId());
        }
    }

    /**
     * 配置 driver point，增删改
     *
     * @param command 操作类型
     * @param point   Point
     */
    private void configurationPoint(String command, Point point) {
        if (Common.Driver.Point.ADD.equals(command) || Common.Driver.Point.UPDATE.equals(command)) {
            driverMetadataService.upsertPoint(point);
        }
        if (Common.Driver.Point.DELETE.equals(command)) {
            driverMetadataService.deletePoint(point.getId(), point.getProfileId());
        }
    }

    /**
     * 配置 driver info，增删改
     *
     * @param command    操作类型
     * @param driverInfo DriverInfo
     */
    private void configurationDriverInfo(String command, DriverInfo driverInfo) {
        if (Common.Driver.DriverInfo.ADD.equals(command) || Common.Driver.DriverInfo.UPDATE.equals(command)) {
            driverMetadataService.upsertDriverInfo(driverInfo);
        }
        if (Common.Driver.DriverInfo.DELETE.equals(command)) {
            driverMetadataService.deleteDriverInfo(driverInfo.getDriverAttributeId(), driverInfo.getProfileId());
        }
    }

    /**
     * 配置 driver point info，增删改
     *
     * @param command   操作类型
     * @param pointInfo PointInfo
     */
    private void configurationPointInfo(String command, PointInfo pointInfo) {
        if (Common.Driver.PointInfo.ADD.equals(command) || Common.Driver.PointInfo.UPDATE.equals(command)) {
            driverMetadataService.upsertPointInfo(pointInfo);
        }
        if (Common.Driver.PointInfo.DELETE.equals(command)) {
            driverMetadataService.deletePointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getPointId());
        }
    }

    private void configurationDriverMetadata(String command, DriverMetadata driverMetadata) {
        if (Common.Driver.Metadata.INIT.equals(command)) {
            log.info("Initialize driver metadata \n{}", JSON.toJSONString(driverMetadata, true));
            driverMetadataService.syncDriverMetadata(driverMetadata);
        }
    }

    @RabbitHandler
    @RabbitListener(queues = "#{driverConfigurationQueue.name}")
    public void driverConfigurationReceive(Channel channel, Message message, DriverConfiguration driverConfiguration) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == driverConfiguration
                    || StringUtils.isEmpty(driverConfiguration.getType())
                    || StringUtils.isEmpty(driverConfiguration.getCommand())
                    || null == driverConfiguration.getContent()
            ) {
                log.error("Invalid driver configuration");
                return;
            }
            log.debug("Configuration from {}", message.getMessageProperties().getReceivedRoutingKey());

            if (Common.Driver.Type.PROFILE.equals(driverConfiguration.getType())) {
                configurationProfile(driverConfiguration.getCommand(), Convert.convert(Profile.class, driverConfiguration.getContent()));
            }
            if (Common.Driver.Type.DEVICE.equals(driverConfiguration.getType())) {
                configurationDevice(driverConfiguration.getCommand(), Convert.convert(Device.class, driverConfiguration.getContent()));
            }
            if (Common.Driver.Type.POINT.equals(driverConfiguration.getType())) {
                configurationPoint(driverConfiguration.getCommand(), Convert.convert(Point.class, driverConfiguration.getContent()));
            }
            if (Common.Driver.Type.DRIVER_INFO.equals(driverConfiguration.getType())) {
                configurationDriverInfo(driverConfiguration.getCommand(), Convert.convert(DriverInfo.class, driverConfiguration.getContent()));
            }
            if (Common.Driver.Type.POINT_INFO.equals(driverConfiguration.getType())) {
                configurationPointInfo(driverConfiguration.getCommand(), Convert.convert(PointInfo.class, driverConfiguration.getContent()));
            }
            if (Common.Driver.Type.METADATA.equals(driverConfiguration.getType())) {
                configurationDriverMetadata(driverConfiguration.getCommand(), Convert.convert(DriverMetadata.class, driverConfiguration.getContent()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
