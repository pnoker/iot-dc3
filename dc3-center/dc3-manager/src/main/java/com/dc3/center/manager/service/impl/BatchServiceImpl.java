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

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.dc3.center.manager.service.*;
import com.dc3.common.bean.batch.BatchDevice;
import com.dc3.common.bean.batch.BatchDriver;
import com.dc3.common.bean.batch.BatchProfile;
import com.dc3.common.constant.Operation;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    public Boolean batchImport(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new ServiceException("Import file is empty");
        }
        try {
            // Convert json file to ImportAll object
            List<BatchDriver> batchDrivers = JSON.parseArray(
                    Dc3Util.inputStreamToString(multipartFile.getInputStream()),
                    BatchDriver.class
            );
            if (null == batchDrivers) {
                throw new ServiceException("Import file is blank");
            }

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
                    addDriverInfo(driver, profile, batchProfile);

                    // Add Point Array
                    addPoint(profile, batchProfile);

                    // Add Device Array
                    addDevice(driver, profile, batchProfile);
                });
            });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * 添加 Profile
     *
     * @param driver        Driver
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

            notifyService.notifyDriverProfile(profile.getId(), Operation.Profile.ADD);
            ThreadUtil.sleep(1, TimeUnit.SECONDS);
        }
        return profile;
    }

    /**
     * 添加 Driver Info 列表
     *
     * @param driver        Driver
     * @param profile       Profile
     * @param batchProfile ImportProfile
     */
    private void addDriverInfo(Driver driver, Profile profile, BatchProfile batchProfile) {
        List<String> dInfos = new ArrayList<>();
        batchProfile.getDriverInfos().forEach(importInfo -> {
            DriverAttribute driverAttribute = driverAttributeService.selectByNameAndDriverId(importInfo.getName(), driver.getId());
            if (null == driverAttribute) {
                throw new ServiceException("Invalid driver info: " + importInfo.getName());
            }
            if (dInfos.contains(importInfo.getName())) {
                throw new ServiceException("Repeatedly driver info: " + importInfo.getName());
            }
            dInfos.add(importInfo.getName());

            DriverInfo driverInfo = driverInfoService.selectByDriverAttributeId(driverAttribute.getId(), profile.getId());
            if (null == driverInfo) {
                driverInfo = new DriverInfo(driverAttribute.getId(), importInfo.getValue(), profile.getId());
                driverInfo.setDescription("批量导入：新增");
                driverInfo = driverInfoService.add(driverInfo);
                if (null == driverInfo) {
                    throw new ServiceException("Add driver info failed: " + importInfo.getName());
                }
                notifyService.notifyDriverDriverInfo(driverInfo.getId(), driverInfo.getDriverAttributeId(), driverInfo.getProfileId(), Operation.DriverInfo.ADD);
            } else {
                driverInfo.setDescription("批量导入：更新");
                driverInfo = driverInfoService.update(driverInfo.setValue(importInfo.getValue()));
                notifyService.notifyDriverDriverInfo(driverInfo.getId(), driverInfo.getDriverAttributeId(), driverInfo.getProfileId(), Operation.DriverInfo.UPDATE);
            }
            ThreadUtil.sleep(1, TimeUnit.SECONDS);
        });
    }

    /**
     * 添加 Point 列表
     *
     * @param profile       Profile
     * @param batchProfile ImportProfile
     */
    private void addPoint(Profile profile, BatchProfile batchProfile) {
        batchProfile.getPoints().forEach(importPoint -> {
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
            }

            ThreadUtil.sleep(1, TimeUnit.SECONDS);
        });
    }

    /**
     * 添加 Device 列表
     *
     * @param driver        Driver
     * @param profile       Profile
     * @param batchProfile ImportProfile
     */
    private void addDevice(Driver driver, Profile profile, BatchProfile batchProfile) {
        batchProfile.getGroups().forEach(importGroup -> {
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
                    device.setDescription("批量导入：新增");
                    device = deviceService.add(device);
                    if (null == device) {
                        throw new ServiceException("Add device failed: " + batchDevice.getName());
                    }
                    notifyService.notifyDriverDevice(device.getId(), device.getProfileId(), Operation.Device.ADD);
                    ThreadUtil.sleep(1, TimeUnit.SECONDS);
                }

                // Add Point Info
                addPointInfo(driver, profile, device, batchDevice);
            });
        });
    }

    /**
     * 添加 Point Info 列表
     *
     * @param driver       Driver
     * @param profile      Profile
     * @param device       Device
     * @param batchDevice ImportDevice
     */
    private void addPointInfo(Driver driver, Profile profile, Device device, BatchDevice batchDevice) {
        batchDevice.getPoints().forEach((pointName, pointInfoList) -> {
            List<String> pInfos = new ArrayList<>();
            pointInfoList.forEach(importInfo -> {
                PointAttribute pointAttribute = pointAttributeService.selectByNameAndDriverId(importInfo.getName(), driver.getId());
                if (null == pointAttribute) {
                    throw new ServiceException("Invalid point info: " + importInfo.getName());
                }
                if (pInfos.contains(importInfo.getName())) {
                    throw new ServiceException("Repeatedly point info: " + importInfo.getName());
                }
                pInfos.add(importInfo.getName());

                Point point = pointService.selectByNameAndProfile(pointName, profile.getId());
                if (null == point) {
                    throw new ServiceException("Point does not exist: " + pointName);
                }

                // If point info does not exist, add a new point info, otherwise point info will be updated
                PointInfo pointInfo = pointInfoService.selectByPointAttributeId(pointAttribute.getId(), device.getId(), point.getId());
                if (null == pointInfo) {
                    pointInfo = new PointInfo(pointAttribute.getId(), importInfo.getValue(), device.getId(), point.getId());
                    pointInfo.setDescription("批量导入：新增");
                    pointInfo = pointInfoService.add(pointInfo);
                    if (null == pointInfo) {
                        throw new ServiceException("Add point info failed: " + importInfo.getName());
                    }
                    notifyService.notifyDriverPointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), Operation.PointInfo.ADD);
                } else {
                    pointInfo.setDescription("批量导入：更新");
                    pointInfo = pointInfoService.update(pointInfo.setValue(importInfo.getValue()));
                    notifyService.notifyDriverPointInfo(pointInfo.getId(), pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), Operation.PointInfo.UPDATE);
                }

                ThreadUtil.sleep(1, TimeUnit.SECONDS);
            });
        });
    }
}
