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

package com.dc3.common.sdk.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import com.dc3.common.bean.driver.AttributeInfo;
import com.dc3.common.bean.driver.DriverEvent;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.bean.DriverProperty;
import com.dc3.common.sdk.service.DriverMetadataService;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Driver Metadata Service Implements
 *
 * @author pnoker
 */
@Slf4j
@Service
@EnableConfigurationProperties({DriverProperty.class})
public class DriverMetadataServiceImpl implements DriverMetadataService {

    @Value("${server.port}")
    private int port;
    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private DriverService driverService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void initial() {
        String localHost = Dc3Util.localHost();
        if (!Dc3Util.isName(driverProperty.getName()) || !Dc3Util.isName(this.serviceName) || !Dc3Util.isHost(localHost)) {
            throw new ServiceException("The driver name, service name or host name format is invalid");
        }
        if (!Dc3Util.isDriverPort(this.port)) {
            throw new ServiceException("The driver port is invalid, port range is 8600-8799");
        }

        Driver driver = new Driver(driverProperty.getName(), this.serviceName, localHost, this.port);
        driver.setDescription(driverProperty.getDescription());
        String routingKey = Common.Rabbit.ROUTING_DRIVER_EVENT_PREFIX + serviceName;
        log.info("The driver {}/{}　is registering...", driver.getServiceName(), driver.getName());

        try {
            threadPoolExecutor.submit(() -> {
                DriverEvent handshakeEvent = new DriverEvent(
                        serviceName,
                        Common.Driver.Event.REGISTER_HANDSHAKE,
                        null
                );
                rabbitTemplate.convertAndSend(
                        Common.Rabbit.TOPIC_EXCHANGE_EVENT,
                        routingKey,
                        handshakeEvent,
                        message -> {
                            message.getMessageProperties().setExpiration("15000");
                            return message;
                        }
                );

                while (!Common.Driver.Status.REGISTERING.equals(driverContext.getDriverStatus())) {
                    ThreadUtil.sleep(500);
                }
            }).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            throw new ServiceException("The driver initialization failed, Check whether dc3-manager are started normally");
        }

        DriverRegister driverRegister = new DriverRegister(
                driver,
                driverProperty.getDriverAttribute(),
                driverProperty.getPointAttribute()
        );
        DriverEvent registerEvent = new DriverEvent(
                serviceName,
                Common.Driver.Event.DRIVER_REGISTER,
                driverRegister
        );
        rabbitTemplate.convertAndSend(
                Common.Rabbit.TOPIC_EXCHANGE_EVENT,
                routingKey,
                registerEvent,
                message -> {
                    message.getMessageProperties().setExpiration("15000");
                    return message;
                }
        );

        try {
            driverContext.setDriverStatus(threadPoolExecutor.submit(() -> {
                DriverEvent syncEvent = new DriverEvent(
                        serviceName,
                        Common.Driver.Event.SYNC_DRIVER_METADATA,
                        driver.getServiceName()
                );
                rabbitTemplate.convertAndSend(
                        Common.Rabbit.TOPIC_EXCHANGE_EVENT,
                        routingKey,
                        syncEvent
                );

                while (!Common.Driver.Status.ONLINE.equals(driverContext.getDriverStatus())) {
                    ThreadUtil.sleep(500);
                }
                return driverContext.getDriverStatus();
            }).get(5, TimeUnit.MINUTES));
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            throw new ServiceException("The driver initialization failed, Sync driver metadata from dc3-manager timeout");
        }

        log.info("Driver initialization is complete");
    }

    @Override
    public void upsertProfile(Profile profile) {
        // Add profile driver info to context
        driverContext.getDriverMetadata().getProfileDriverInfoMap().computeIfAbsent(profile.getId(), k -> new ConcurrentHashMap<>(16));

        // Add profile point to context
        driverContext.getDriverMetadata().getProfilePointMap().computeIfAbsent(profile.getId(), k -> new ConcurrentHashMap<>(16));

        log.info("Upsert profile {}", profile);
    }

    @Override
    public void deleteProfile(Long id) {
        log.info("Delete profile {}, driverInfo {}, profilePoint {}", id, driverContext.getDriverMetadata().getProfileDriverInfoMap().get(id), driverContext.getDriverMetadata().getProfilePointMap().get(id));

        driverContext.getDriverMetadata().getProfileDriverInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDriverMetadata().getProfilePointMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void upsertDevice(Device device) {
        // Add device to context
        driverContext.getDriverMetadata().getDeviceMap().put(device.getId(), device);
        // Add device name to context
        driverContext.getDriverMetadata().getDeviceNameMap().put(device.getName(), device.getId());

        log.info("Upsert device {}", device);
    }

    @Override
    public void deleteDevice(Long id) {
        log.info("Delete device {}, devicePointInfo {}", driverContext.getDriverMetadata().getDeviceMap().get(id), driverContext.getDriverMetadata().getDevicePointInfoMap().get(id));

        driverContext.getDriverMetadata().getDeviceMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDriverMetadata().getDeviceNameMap().entrySet().removeIf(next -> next.getValue().equals(id));
        driverContext.getDriverMetadata().getDevicePointInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void upsertPoint(Point point) {
        // Upsert point to profile point map context
        log.info("Upsert point {}", point);
        driverContext.getDriverMetadata().getProfilePointMap().computeIfAbsent(point.getProfileId(), k -> new HashMap<>(16))
                .put(point.getId(), point);
    }

    @Override
    public void deletePoint(Long pointId, Long profileId) {
        // Delete point from profile point map context
        driverContext.getDriverMetadata().getProfilePointMap().computeIfPresent(profileId, (k, v) -> {
            v.entrySet().removeIf(next -> {
                boolean equals = next.getKey().equals(pointId);
                if (equals) {
                    log.info("Delete point {}", next.getValue());
                }
                return equals;
            });
            return v;
        });
    }

    @Override
    public void upsertDriverInfo(DriverInfo driverInfo) {
        // Add driver info to driver info map context
        DriverAttribute attribute = driverContext.getDriverMetadata().getDriverAttributeMap().get(driverInfo.getDriverAttributeId());
        if (null != attribute) {
            log.info("Upsert driver info {}", driverInfo);
            driverContext.getDriverMetadata().getProfileDriverInfoMap().computeIfAbsent(driverInfo.getProfileId(), k -> new HashMap<>(16))
                    .put(attribute.getName(), new AttributeInfo(driverInfo.getValue(), attribute.getType()));
        }
    }

    @Override
    public void deleteDriverInfo(Long attributeId, Long profileId) {
        DriverAttribute attribute = driverContext.getDriverMetadata().getDriverAttributeMap().get(attributeId);
        if (null != attribute) {
            String attributeName = attribute.getName();

            // Delete driver info from driver info map context
            driverContext.getDriverMetadata().getProfileDriverInfoMap().computeIfPresent(profileId, (k, v) -> {
                v.entrySet().removeIf(next -> {
                    boolean equals = next.getKey().equals(attributeName);
                    if (equals) {
                        log.info("Delete driver info {}", next.getValue());
                    }
                    return equals;
                });
                return v;
            });
        }
    }

    @Override
    public void upsertPointInfo(PointInfo pointInfo) {
        // Add the point info to the device point info map context
        PointAttribute attribute = driverContext.getDriverMetadata().getPointAttributeMap().get(pointInfo.getPointAttributeId());
        if (null != attribute) {
            log.info("Upsert point info {}", pointInfo);
            driverContext.getDriverMetadata().getDevicePointInfoMap().computeIfAbsent(pointInfo.getDeviceId(), k -> new HashMap<>(16))
                    .computeIfAbsent(pointInfo.getPointId(), k -> new HashMap<>(16))
                    .put(attribute.getName(), new AttributeInfo(pointInfo.getValue(), attribute.getType()));
        }
    }

    @Override
    public void deletePointInfo(Long pointId, Long attributeId, Long deviceId) {
        PointAttribute attribute = driverContext.getDriverMetadata().getPointAttributeMap().get(attributeId);
        if (null != attribute) {
            String attributeName = attribute.getName();

            // Delete the point info from the device info map context
            driverContext.getDriverMetadata().getDevicePointInfoMap().computeIfPresent(deviceId, (k1, v1) -> {
                v1.computeIfPresent(pointId, (k2, v2) -> {
                    v2.entrySet().removeIf(next -> {
                        boolean equals = next.getKey().equals(attributeName);
                        if (equals) {
                            log.info("Delete point info {}", next.getValue());
                        }
                        return equals;
                    });
                    return v2;
                });
                return v1;
            });

            // If the point attribute is null, delete the point info from the device info map context
            driverContext.getDriverMetadata().getDevicePointInfoMap().computeIfPresent(deviceId, (k, v) -> {
                v.entrySet().removeIf(next -> next.getValue().size() < 1);
                return v;
            });
        }
    }
}
