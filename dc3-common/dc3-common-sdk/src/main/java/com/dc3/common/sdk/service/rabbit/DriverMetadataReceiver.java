/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.service.rabbit;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.bean.driver.DriverMetadata;
import com.dc3.common.constant.CommonConstant;
import com.dc3.common.model.*;
import com.dc3.common.sdk.bean.driver.DriverContext;
import com.dc3.common.sdk.service.DriverMetadataService;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.utils.JsonUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
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
public class DriverMetadataReceiver {

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverMetadataService driverMetadataService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverMetadataQueue.name}")
    public void driverConfigurationReceive(Channel channel, Message message, DriverConfiguration driverConfiguration) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == driverConfiguration || StrUtil.isEmpty(driverConfiguration.getType()) || StrUtil.isEmpty(driverConfiguration.getCommand())) {
                log.error("Invalid driver configuration {}", driverConfiguration);
                return;
            }

            switch (driverConfiguration.getType()) {
                case CommonConstant.Driver.Type.DRIVER:
                    configurationDriver(driverConfiguration);
                    break;
                case CommonConstant.Driver.Type.PROFILE:
                    configurationProfile(driverConfiguration);
                    break;
                case CommonConstant.Driver.Type.DEVICE:
                    configurationDevice(driverConfiguration);
                    break;
                case CommonConstant.Driver.Type.POINT:
                    configurationPoint(driverConfiguration);
                    break;
                case CommonConstant.Driver.Type.DRIVER_INFO:
                    configurationDriverInfo(driverConfiguration);
                    break;
                case CommonConstant.Driver.Type.POINT_INFO:
                    configurationPointInfo(driverConfiguration);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 配置 driver
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriver(DriverConfiguration driverConfiguration) {
        if (!CommonConstant.Response.OK.equals(driverConfiguration.getResponse())) {
            driverService.close("The driver initialization failed: {}", driverConfiguration.getResponse());
        }

        switch (driverConfiguration.getCommand()) {
            case CommonConstant.Driver.Event.DRIVER_HANDSHAKE_BACK:
                driverContext.setDriverStatus(CommonConstant.Status.REGISTERING);
                break;
            case CommonConstant.Driver.Event.DRIVER_REGISTER_BACK:
                driverContext.setDriverStatus(CommonConstant.Status.ONLINE);
                break;
            case CommonConstant.Driver.Event.DRIVER_METADATA_SYNC_BACK:
                DriverMetadata driverMetadata = Convert.convert(DriverMetadata.class, driverConfiguration.getContent());
                log.debug("Initialization driver metadata: {}", JsonUtil.toPrettyJsonString(driverMetadata));
                driverContext.setDriverMetadata(driverMetadata);
                break;
            default:
                break;
        }
    }

    /**
     * 配置 driver profile
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationProfile(DriverConfiguration driverConfiguration) {
        Profile profile = Convert.convert(Profile.class, driverConfiguration.getContent());
        if (CommonConstant.Driver.Profile.ADD.equals(driverConfiguration.getCommand()) || CommonConstant.Driver.Profile.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert profile \n{}", JsonUtil.toJsonString(profile));
            driverMetadataService.upsertProfile(profile);
        } else if (CommonConstant.Driver.Profile.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete profile {}", profile.getName());
            driverMetadataService.deleteProfile(profile.getId());
        }
    }

    /**
     * 配置 driver device
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDevice(DriverConfiguration driverConfiguration) {
        Device device = Convert.convert(Device.class, driverConfiguration.getContent());
        if (CommonConstant.Driver.Device.ADD.equals(driverConfiguration.getCommand()) || CommonConstant.Driver.Device.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert device \n{}", JsonUtil.toJsonString(device));
            driverMetadataService.upsertDevice(device);
        } else if (CommonConstant.Driver.Device.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete device {}", device.getName());
            driverMetadataService.deleteDevice(device.getId());
        }
    }

    /**
     * 配置 driver point
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPoint(DriverConfiguration driverConfiguration) {
        Point point = Convert.convert(Point.class, driverConfiguration.getContent());
        if (CommonConstant.Driver.Point.ADD.equals(driverConfiguration.getCommand()) || CommonConstant.Driver.Point.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert point \n{}", JsonUtil.toJsonString(point));
            driverMetadataService.upsertPoint(point);
        } else if (CommonConstant.Driver.Point.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete point {}", point.getName());
            driverMetadataService.deletePoint(point.getProfileId(), point.getId());
        }
    }

    /**
     * 配置 driver info
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationDriverInfo(DriverConfiguration driverConfiguration) {
        DriverInfo driverInfo = Convert.convert(DriverInfo.class, driverConfiguration.getContent());
        if (CommonConstant.Driver.DriverInfo.ADD.equals(driverConfiguration.getCommand()) || CommonConstant.Driver.DriverInfo.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert driver info \n{}", JsonUtil.toJsonString(driverInfo));
            driverMetadataService.upsertDriverInfo(driverInfo);
        } else if (CommonConstant.Driver.DriverInfo.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete driver info {}", driverInfo);
            driverMetadataService.deleteDriverInfo(driverInfo.getDeviceId(), driverInfo.getDriverAttributeId());
        }
    }

    /**
     * 配置 driver point info
     *
     * @param driverConfiguration DriverConfiguration
     */
    private void configurationPointInfo(DriverConfiguration driverConfiguration) {
        PointInfo pointInfo = Convert.convert(PointInfo.class, driverConfiguration.getContent());
        if (CommonConstant.Driver.PointInfo.ADD.equals(driverConfiguration.getCommand()) || CommonConstant.Driver.PointInfo.UPDATE.equals(driverConfiguration.getCommand())) {
            log.info("Upsert point info \n{}", JsonUtil.toJsonString(pointInfo));
            driverMetadataService.upsertPointInfo(pointInfo);
        } else if (CommonConstant.Driver.PointInfo.DELETE.equals(driverConfiguration.getCommand())) {
            log.info("Delete point info {}", pointInfo);
            driverMetadataService.deletePointInfo(pointInfo.getPointId(), pointInfo.getId(), pointInfo.getPointAttributeId());
        }
    }

}
