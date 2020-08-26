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

package com.dc3.center.manager.service.impl;

import com.dc3.center.manager.service.*;
import com.dc3.common.bean.batch.BatchDriver;
import com.dc3.common.bean.batch.BatchGroup;
import com.dc3.common.bean.batch.BatchPoint;
import com.dc3.common.bean.batch.BatchProfile;
import com.dc3.common.constant.Operation;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>BatchService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class BatchServiceImpl implements BatchService {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverInfoService driverInfoService;
    @Resource
    private ProfileService profileService;
    @Resource
    private PointService pointService;
    @Resource
    private GroupService groupService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointInfoService pointInfoService;
    @Resource
    private NotifyService notifyService;

    @Override
    @Transactional
    public Boolean batchImport(List<BatchDriver> batchDrivers) {
        try {
            batchDrivers.forEach(batchDriver -> {
                if (StringUtils.isBlank(batchDriver.getServiceName())) {
                    throw new ServiceException("Driver service name is blank");
                }
                Driver driver = driverService.selectByServiceName(batchDriver.getServiceName());
                if (null == driver) {
                    throw new ServiceException("Driver service does not exist: " + batchDriver.getServiceName());
                }

                batchDriver.getProfiles().forEach(batchProfile -> {

                    // Add Profile
                    Profile profile = addProfile(driver, batchProfile);

                    // Add Driver Info Array
                    addDriverInfo(driver, profile, batchProfile.getDriverConfig());

                    // Add Point Array
                    addPoint(profile, batchProfile.getPoints());

                    // Add Device Array
                    addDevice(driver, profile, batchProfile.getGroups(), profile.getShare(), batchProfile.getPointConfig());
                });
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * 添加 Profile
     *
     * @param driver       Driver
     * @param batchProfile ImportProfile
     */
    private Profile addProfile(Driver driver, BatchProfile batchProfile) {
        if (StringUtils.isBlank(batchProfile.getName())) {
            throw new ServiceException("Profile name is blank");
        }

        Profile profile = profileService.selectByName(batchProfile.getName());
        if (null == profile) {
            profile = new Profile(batchProfile.getName(), batchProfile.getShare(), driver.getId());
            profile.setDescription("批量导入：新增");
            profile = profileService.add(profile);
            if (null == profile) {
                throw new ServiceException("Add profile failed: " + batchProfile.getName());
            }

            notifyService.notifyDriverProfile(driver, profile.getId(), Operation.Profile.ADD);
        }
        return profile;
    }

    /**
     * 添加 Driver Info 列表
     *
     * @param driver       Driver
     * @param profile      Profile
     * @param driverConfig Driver Config
     */
    private void addDriverInfo(Driver driver, Profile profile, Map<String, String> driverConfig) {
        List<String> driverInfoList = new ArrayList<>();
        if (null == driverConfig) {
            return;
        }
        driverConfig.forEach((name, value) -> {
            DriverAttribute driverAttribute = driverAttributeService.selectByNameAndDriverId(name, driver.getId());
            if (null == driverAttribute) {
                throw new ServiceException("Invalid driver info: " + name);
            }
            if (driverInfoList.contains(name)) {
                throw new ServiceException("Repeatedly driver info: " + name);
            }
            driverInfoList.add(name);

            DriverInfo driverInfo = driverInfoService.selectByDriverAttributeId(driverAttribute.getId(), profile.getId());
            if (null == driverInfo) {
                driverInfo = new DriverInfo(driverAttribute.getId(), value, profile.getId());
                driverInfo.setDescription("批量导入：新增");
                driverInfo = driverInfoService.add(driverInfo);
                if (null == driverInfo) {
                    throw new ServiceException("Add driver info failed: " + name);
                }
                notifyService.notifyDriverDriverInfo(driverInfo.getId(), driverInfo.getDriverAttributeId(), driverInfo.getProfileId(), Operation.DriverInfo.ADD);
            } else {
                driverInfo.setDescription("批量导入：更新");
                driverInfo = driverInfoService.update(driverInfo.setValue(value));
                notifyService.notifyDriverDriverInfo(driverInfo.getId(), driverInfo.getDriverAttributeId(), driverInfo.getProfileId(), Operation.DriverInfo.UPDATE);
            }
        });
    }

    /**
     * 添加 Point 列表
     *
     * @param profile Profile
     * @param points  Point Array
     */
    private void addPoint(Profile profile, List<BatchPoint> points) {
        points.forEach(importPoint -> {
            // If point does not exist, add a new point, otherwise point will be updated
            Point point = pointService.selectByNameAndProfile(importPoint.getName(), profile.getId());
            if (null == point) {
                point = new Point(
                        importPoint.getName(),
                        importPoint.getType(),
                        importPoint.getRw(),
                        importPoint.getBase(),
                        importPoint.getMinimum(),
                        importPoint.getMaximum(),
                        importPoint.getMultiple(),
                        importPoint.getAccrue(),
                        importPoint.getFormat(),
                        importPoint.getUnit(),
                        profile.getId()
                );
                point.setDescription("批量导入：新增");
                point = pointService.add(point);
                if (null == point) {
                    throw new ServiceException("Add point failed: " + importPoint.getName());
                }
                notifyService.notifyDriverPoint(point.getId(), point.getProfileId(), Operation.Point.ADD);
            } else {
                point
                        .setName(importPoint.getName())
                        .setType(importPoint.getType())
                        .setRw(importPoint.getRw())
                        .setBase(importPoint.getBase())
                        .setMinimum(importPoint.getMinimum())
                        .setMaximum(importPoint.getMaximum())
                        .setMultiple(importPoint.getMultiple())
                        .setAccrue(importPoint.getAccrue())
                        .setFormat(importPoint.getFormat())
                        .setUnit(importPoint.getUnit());
                point.setDescription("批量导入：更新");
                pointService.update(point);
                notifyService.notifyDriverPoint(point.getId(), point.getProfileId(), Operation.Point.UPDATE);
            }
        });
    }

    /**
     * 添加 Device 列表
     *
     * @param driver  Driver
     * @param profile Profile
     * @param groups  Device Group
     */
    private void addDevice(Driver driver, Profile profile, List<BatchGroup> groups, boolean share, Map<String, Map<String, String>> pointConfig) {
        groups.forEach(importGroup -> {
            // If group does not exist, add a new group
            Group group = groupService.selectByName(importGroup.getName());
            if (null == group) {
                group = new Group(importGroup.getName());
                group = groupService.add(group);
                if (null == group) {
                    throw new ServiceException("Add group failed: " + importGroup.getName());
                }
            }

            Group finalGroup = group;
            importGroup.getDevices().forEach(batchDevice -> {
                Device device = deviceService.selectDeviceByNameAndGroup(batchDevice.getName(), finalGroup.getId());
                if (null == device) {
                    device = new Device(batchDevice.getName(), profile.getId(), finalGroup.getId());
                    if (batchDevice.getMulti()) {
                        device.setMulti(true);
                    }
                    device.setDescription("批量导入：新增");
                    device = deviceService.add(device);
                    if (null == device) {
                        throw new ServiceException("Add device failed: " + batchDevice.getName());
                    }
                    notifyService.notifyDriverDevice(device.getId(), device.getProfileId(), Operation.Device.ADD);
                }

                // Add Point Info
                if (share) {
                    addPointInfo(driver, profile, device, pointConfig);
                } else {
                    addPointInfo(driver, profile, device, batchDevice.getPointConfig());
                }
            });
        });
    }

    /**
     * 添加 Point Info 列表
     *
     * @param driver      Driver
     * @param profile     Profile
     * @param device      Device
     * @param pointConfig Point Config Map
     */
    private void addPointInfo(Driver driver, Profile profile, Device device, Map<String, Map<String, String>> pointConfig) {
        pointConfig.forEach((pointName, pointConfigMap) -> {
            List<String> pointInfoList = new ArrayList<>();
            pointConfigMap.forEach((name, value) -> {
                PointAttribute pointAttribute = pointAttributeService.selectByNameAndDriverId(name, driver.getId());
                if (null == pointAttribute) {
                    throw new ServiceException("Invalid point info: " + name);
                }
                if (pointInfoList.contains(name)) {
                    throw new ServiceException("Repeatedly point info: " + name);
                }
                pointInfoList.add(name);

                Point point = pointService.selectByNameAndProfile(pointName, profile.getId());
                if (null == point) {
                    throw new ServiceException("Point does not exist: " + pointName);
                }

                // If point info does not exist, add a new point info, otherwise point info will be updated
                PointInfo pointInfo = pointInfoService.selectByPointAttributeId(pointAttribute.getId(), device.getId(), point.getId());
                if (null == pointInfo) {
                    pointInfo = new PointInfo(pointAttribute.getId(), value, device.getId(), point.getId());
                    pointInfo.setDescription("批量导入：新增");
                    pointInfo = pointInfoService.add(pointInfo);
                    if (null == pointInfo) {
                        throw new ServiceException("Add point info failed: " + name);
                    }
                    notifyService.notifyDriverPointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), Operation.PointInfo.ADD);
                } else {
                    pointInfo.setDescription("批量导入：更新");
                    pointInfo = pointInfoService.update(pointInfo.setValue(value));
                    notifyService.notifyDriverPointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), Operation.PointInfo.UPDATE);
                }
            });
        });
    }
}
