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
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverMetadataService;
import com.dc3.common.sdk.service.DriverService;
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
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverMetadataService driverMetadataService;

    /**
     * 配置 driver
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriver(String command, DriverConfiguration driverConfiguration) {
        if (Common.Driver.Event.REGISTER_HANDSHAKE_BACK.equals(command)) {
            if (Common.Response.OK.equals(driverConfiguration.getResponse())) {
                driverContext.setDriverStatus(Common.Driver.Status.REGISTERING);
            } else {
                throw new ServiceException("The driver initialization failed, Please check whether dc3-manager are started normally");
            }
        }

        if (Common.Driver.Event.DRIVER_REGISTER_BACK.equals(command)) {
            if (Common.Response.OK.equals(driverConfiguration.getResponse())) {
                driverContext.setDriverStatus(Common.Driver.Status.ONLINE);
            } else {
                throw new ServiceException("Driver registration failed, " + driverConfiguration.getResponse());
            }
        }

        if (Common.Driver.Event.SYNC_DRIVER_METADATA_BACK.equals(command)) {
            if (Common.Response.OK.equals(driverConfiguration.getResponse())) {
                driverContext.setDriverMetadata(Convert.convert(DriverMetadata.class, driverConfiguration.getContent()));
            } else {
                throw new ServiceException("Driver registration failed, " + driverConfiguration.getResponse());
            }
        }
    }

    /**
     * 配置 driver profile，增删改
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationProfile(String command, DriverConfiguration driverConfiguration) {
        Profile profile = Convert.convert(Profile.class, driverConfiguration.getContent());
        if (Common.Driver.Profile.ADD.equals(command) || Common.Driver.Profile.UPDATE.equals(command)) {
            log.debug("Upsert profile \n{}", JSON.toJSONString(profile, true));
            driverMetadataService.upsertProfile(profile);
        }
        if (Common.Driver.Profile.DELETE.equals(command)) {
            log.debug("Delete profile {}", profile.getName());
            driverMetadataService.deleteProfile(profile.getId());
        }
    }

    /**
     * 配置 driver device，增删改
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDevice(String command, DriverConfiguration driverConfiguration) {
        Device device = Convert.convert(Device.class, driverConfiguration.getContent());
        if (Common.Driver.Device.ADD.equals(command) || Common.Driver.Device.UPDATE.equals(command)) {
            log.debug("Upsert device \n{}", JSON.toJSONString(device, true));
            driverMetadataService.upsertDevice(device);
        }
        if (Common.Driver.Device.DELETE.equals(command)) {
            log.debug("Delete device {}", device.getName());
            driverMetadataService.deleteDevice(device.getId());
        }
    }

    /**
     * 配置 driver point，增删改
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPoint(String command, DriverConfiguration driverConfiguration) {
        Point point = Convert.convert(Point.class, driverConfiguration.getContent());
        if (Common.Driver.Point.ADD.equals(command) || Common.Driver.Point.UPDATE.equals(command)) {
            log.debug("Upsert point \n{}", JSON.toJSONString(point, true));
            driverMetadataService.upsertPoint(point);
        }
        if (Common.Driver.Point.DELETE.equals(command)) {
            log.debug("Delete point {}", point.getName());
            driverMetadataService.deletePoint(point.getId(), point.getProfileId());
        }
    }

    /**
     * 配置 driver info，增删改
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriverInfo(String command, DriverConfiguration driverConfiguration) {
        DriverInfo driverInfo = Convert.convert(DriverInfo.class, driverConfiguration.getContent());
        if (Common.Driver.DriverInfo.ADD.equals(command) || Common.Driver.DriverInfo.UPDATE.equals(command)) {
            log.debug("Upsert driver info \n{}", JSON.toJSONString(driverInfo, true));
            driverMetadataService.upsertDriverInfo(driverInfo);
        }
        if (Common.Driver.DriverInfo.DELETE.equals(command)) {
            log.debug("Delete driver info {}", driverInfo);
            driverMetadataService.deleteDriverInfo(driverInfo.getDriverAttributeId(), driverInfo.getProfileId());
        }
    }

    /**
     * 配置 driver point info，增删改
     *
     * @param command             操作类型
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPointInfo(String command, DriverConfiguration driverConfiguration) {
        PointInfo pointInfo = Convert.convert(PointInfo.class, driverConfiguration.getContent());
        if (Common.Driver.PointInfo.ADD.equals(command) || Common.Driver.PointInfo.UPDATE.equals(command)) {
            log.debug("Upsert point info \n{}", JSON.toJSONString(pointInfo, true));
            driverMetadataService.upsertPointInfo(pointInfo);
        }
        if (Common.Driver.PointInfo.DELETE.equals(command)) {
            log.debug("Delete point info {}", pointInfo);
            driverMetadataService.deletePointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getPointId());
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
            ) {
                log.error("Invalid driver configuration");
                return;
            }
            log.debug("Driver Configuration from {}", message.getMessageProperties().getReceivedRoutingKey());

            if (Common.Driver.Type.DRIVER.equals(driverConfiguration.getType())) {
                configurationDriver(driverConfiguration.getCommand(), driverConfiguration);
            }
            if (Common.Driver.Type.PROFILE.equals(driverConfiguration.getType())) {
                configurationProfile(driverConfiguration.getCommand(), driverConfiguration);
            }
            if (Common.Driver.Type.DEVICE.equals(driverConfiguration.getType())) {
                configurationDevice(driverConfiguration.getCommand(), driverConfiguration);
            }
            if (Common.Driver.Type.POINT.equals(driverConfiguration.getType())) {
                configurationPoint(driverConfiguration.getCommand(), driverConfiguration);
            }
            if (Common.Driver.Type.DRIVER_INFO.equals(driverConfiguration.getType())) {
                configurationDriverInfo(driverConfiguration.getCommand(), driverConfiguration);
            }
            if (Common.Driver.Type.POINT_INFO.equals(driverConfiguration.getType())) {
                configurationPointInfo(driverConfiguration.getCommand(), driverConfiguration);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
