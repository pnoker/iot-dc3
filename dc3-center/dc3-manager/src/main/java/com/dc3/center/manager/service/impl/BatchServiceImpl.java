/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
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

package com.dc3.center.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dc3.center.manager.service.*;
import com.dc3.common.bean.batch.*;
import com.dc3.common.bean.driver.AttributeInfo;
import com.dc3.common.bean.driver.DriverMetadata;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private GroupService groupService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointInfoService pointInfoService;

    @Resource
    private NotifyService notifyService;

    @Override
    @Transactional
    public void batchImport(List<BatchDriver> batchDrivers) {
        batchDrivers.forEach(batchDriver -> {
            if (StrUtil.isBlank(batchDriver.getServiceName())) {
                throw new ServiceException("Driver service name is blank");
            }
            Driver driver = driverService.selectByServiceName(batchDriver.getServiceName());
            if (null == driver) {
                throw new ServiceException("Driver service does not exist: " + batchDriver.getServiceName());
            }

            batchDriver.getProfiles().forEach(batchProfile -> {

                // import Profile
                Profile profile = importProfile(driver, batchProfile);

                // import Driver Info Array
                importDriverInfo(driver, profile, batchProfile.getDriverConfig());

                // import Point Array
                importPoint(profile, batchProfile.getPoints());

                // import Device Array
                importDevice(driver, profile, batchProfile.getGroups(), batchProfile.getPointConfig());
            });
        });
    }

    @Override
    public BatchDriver batchExport(String serviceName) {
        Driver driver = driverService.selectByServiceName(serviceName);

        BatchDriver batchDriver = new BatchDriver();
        batchDriver.setServiceName(serviceName);

        // export profile
        List<BatchProfile> profiles = exportProfile(driver.getId());
        if (profiles.size() > 0) {
            batchDriver.setProfiles(profiles);
        }

        return batchDriver;
    }

    @Override
    public DriverMetadata batchDriverMetadata(String serviceName) {
        DriverMetadata driverMetadata = new DriverMetadata();
        try {
            Driver driver = driverService.selectByServiceName(serviceName);
            Map<Long, DriverAttribute> driverAttributeMap = getDriverAttributeMap(driver.getId());
            driverMetadata.setDriverAttributeMap(driverAttributeMap);

            Map<Long, PointAttribute> pointAttributeMap = getPointAttributeMap(driver.getId());
            driverMetadata.setPointAttributeMap(pointAttributeMap);

            List<Profile> profiles = profileService.selectByDriverId(driver.getId());
            List<Long> profileList = profiles.stream().map(Description::getId).collect(Collectors.toList());

            Map<Long, Map<String, AttributeInfo>> profileDriverInfoMap = getProfileDriverInfoMap(profileList, driverAttributeMap);
            driverMetadata.setProfileDriverInfoMap(profileDriverInfoMap);

            Map<Long, Device> deviceMap = new ConcurrentHashMap<>(16);
            Map<String, Long> deviceNameMap = new ConcurrentHashMap<>(16);
            profileList.forEach(profileId -> deviceService.selectDeviceByProfileId(profileId)
                    .forEach(device -> {
                        deviceMap.put(device.getId(), device);
                        deviceNameMap.put(device.getName(), device.getId());
                    }));
            driverMetadata.setDeviceMap(deviceMap);
            driverMetadata.setDeviceNameMap(deviceNameMap);

            Map<Long, Map<Long, Point>> profilePointMap = getProfilePointMap(profileList);
            driverMetadata.setProfilePointMap(profilePointMap);

            Map<Long, Map<Long, Map<String, AttributeInfo>>> devicePointInfoMap = new ConcurrentHashMap<>(16);
            Map<Long, Map<String, Long>> devicePointNameMap = new ConcurrentHashMap<>(16);
            deviceMap.values().forEach(device -> {
                Map<Long, Map<String, AttributeInfo>> infoMap = getDevicePointInfoMap(device, profilePointMap, pointAttributeMap);
                if (infoMap.size() > 0) {
                    devicePointInfoMap.put(device.getId(), infoMap);
                }
                Map<String, Long> nameMap = getPointNameMap(device.getProfileId());
                if (nameMap.size() > 0) {
                    devicePointNameMap.put(device.getId(), nameMap);
                }
            });
            driverMetadata.setDevicePointInfoMap(devicePointInfoMap);
            driverMetadata.setDevicePointNameMap(devicePointNameMap);
            return driverMetadata;
        } catch (NotFoundException ignored) {
        }
        return driverMetadata;
    }

    /**
     * 导入 Profile
     *
     * @param driver       Driver
     * @param batchProfile BatchProfile
     */
    private Profile importProfile(Driver driver, BatchProfile batchProfile) {
        if (StrUtil.isBlank(batchProfile.getName())) {
            throw new ServiceException("Profile name is blank");
        }

        Profile profile;
        try {
            profile = profileService.selectByName(batchProfile.getName());
            profile.setShare(batchProfile.getShare());
            profile.setDescription("批量导入：更新操作");
            profile = profileService.update(profile);
            notifyService.notifyDriverProfile(Common.Driver.Profile.UPDATE, profile);
        } catch (NotFoundException notFoundException) {
            profile = new Profile(batchProfile.getName(), batchProfile.getShare(), driver.getId());
            profile.setDescription("批量导入：新增操作");
            profile = profileService.add(profile);
            notifyService.notifyDriverProfile(Common.Driver.Profile.ADD, profile);
        }

        return profile;
    }

    /**
     * 导入 Driver Info 列表
     *
     * @param driver       Driver
     * @param profile      Profile
     * @param driverConfig Driver Config
     */
    private void importDriverInfo(Driver driver, Profile profile, final Map<String, String> driverConfig) {
        List<String> driverInfoList = new ArrayList<>(16);
        if (null == driverConfig) {
            return;
        }

        driverConfig.forEach((name, value) -> {
            DriverAttribute driverAttribute = driverAttributeService.selectByNameAndDriverId(name, driver.getId());
            if (driverInfoList.contains(name)) {
                throw new ServiceException("Repeatedly driver info: " + name);
            }
            driverInfoList.add(name);

            try {
                DriverInfo driverInfo = driverInfoService.selectByAttributeIdAndProfileId(driverAttribute.getId(), profile.getId());
                driverInfo.setDescription("批量导入：更新操作");
                driverInfo = driverInfoService.update(driverInfo.setValue(value));
                notifyService.notifyDriverDriverInfo(Common.Driver.DriverInfo.UPDATE, driverInfo);
            } catch (NotFoundException notFoundException) {
                DriverInfo driverInfo = new DriverInfo(driverAttribute.getId(), value, profile.getId());
                driverInfo.setDescription("批量导入：新增操作");
                driverInfo = driverInfoService.add(driverInfo);
                notifyService.notifyDriverDriverInfo(Common.Driver.DriverInfo.ADD, driverInfo);
            }
        });
    }

    /**
     * 导入 Point 列表
     *
     * @param profile Profile
     * @param points  Point Array
     */
    private void importPoint(Profile profile, List<BatchPoint> points) {
        points.forEach(importPoint -> {
            // If point does not exist, add a new point, otherwise point will be updated
            try {
                Point point = pointService.selectByNameAndProfileId(importPoint.getName(), profile.getId());
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
                point.setDescription("批量导入：更新操作");
                pointService.update(point);
                notifyService.notifyDriverPoint(Common.Driver.Point.UPDATE, point);
            } catch (NotFoundException notFoundException) {
                Point point = new Point(
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
                point.setDescription("批量导入：新增操作");
                point = pointService.add(point);
                notifyService.notifyDriverPoint(Common.Driver.Point.ADD, point);
            }
        });
    }

    /**
     * 导入 Device 列表
     *
     * @param driver  Driver
     * @param profile Profile
     * @param groups  Device Group
     */
    private void importDevice(Driver driver, Profile profile, List<BatchGroup> groups, final Map<String, Map<String, String>> pointConfig) {
        groups.forEach(importGroup -> {
            // If group does not exist, add a new group
            Group groupTemp;
            try {
                groupTemp = groupService.selectByName(importGroup.getName());
            } catch (NotFoundException notFoundException) {
                groupTemp = new Group(importGroup.getName());
                groupTemp = groupService.add(groupTemp);
            }

            final Group group = groupTemp;
            importGroup.getDevices().forEach(batchDevice -> {
                Device device;
                try {
                    device = deviceService.selectDeviceByNameAndGroupId(batchDevice.getName(), group.getId());
                    device.setMulti(batchDevice.getMulti());
                    device.setDescription("批量导入：更新操作");
                    deviceService.update(device);
                    notifyService.notifyDriverDevice(Common.Driver.Device.UPDATE, device);
                } catch (NotFoundException notFoundException) {
                    device = new Device(batchDevice.getName(), profile.getId(), group.getId());
                    if (batchDevice.getMulti()) {
                        device.setMulti(true);
                    }
                    device.setDescription("批量导入：新增操作");
                    device = deviceService.add(device);
                    notifyService.notifyDriverDevice(Common.Driver.Device.ADD, device);
                }

                // Upsert Point Info
                if (profile.getShare()) {
                    importPointInfo(driver, profile, device, pointConfig);
                } else {
                    importPointInfo(driver, profile, device, batchDevice.getPointConfig());
                }
            });
        });
    }

    /**
     * 导入 Point Info 列表
     *
     * @param driver      Driver
     * @param profile     Profile
     * @param device      Device
     * @param pointConfig Point Config Map
     */
    private void importPointInfo(Driver driver, Profile profile, Device device, final Map<String, Map<String, String>> pointConfig) {
        pointConfig.forEach((pointName, pointConfigMap) -> {
            List<String> pointInfoList = new ArrayList<>(16);
            pointConfigMap.forEach((name, value) -> {
                PointAttribute pointAttribute = pointAttributeService.selectByNameAndDriverId(name, driver.getId());
                if (pointInfoList.contains(name)) {
                    throw new ServiceException("Repeatedly point info: " + name);
                }
                pointInfoList.add(name);

                Point point = pointService.selectByNameAndProfileId(pointName, profile.getId());

                // If point info does not exist, add a new point info, otherwise point info will be updated
                try {
                    PointInfo pointInfo = pointInfoService.selectByAttributeIdAndDeviceIdAndPointId(pointAttribute.getId(), device.getId(), point.getId());
                    pointInfo.setDescription("批量导入：更新操作");
                    pointInfo = pointInfoService.update(pointInfo.setValue(value));
                    notifyService.notifyDriverPointInfo(Common.Driver.PointInfo.UPDATE, pointInfo);
                } catch (NotFoundException notFoundException) {
                    PointInfo pointInfo = new PointInfo(pointAttribute.getId(), value, device.getId(), point.getId());
                    pointInfo.setDescription("批量导入：新增操作");
                    pointInfo = pointInfoService.add(pointInfo);
                    notifyService.notifyDriverPointInfo(Common.Driver.PointInfo.ADD, pointInfo);
                }
            });
        });
    }

    /**
     * 导出 Profile
     *
     * @param driverId Driver Id
     * @return BatchProfile Array
     */
    private List<BatchProfile> exportProfile(Long driverId) {
        List<BatchProfile> batchProfiles = new ArrayList<>(16);

        profileService.selectByDriverId(driverId)
                .forEach(profile -> {
                    BatchProfile batchProfile = new BatchProfile();
                    batchProfile.setName(profile.getName());

                    // todo export share
                    batchProfile.setShare(false);

                    // export driver config
                    Map<String, String> driverConfig = exportDriverConfig(driverId, profile.getId());
                    if (driverConfig.size() > 0) {
                        batchProfile.setDriverConfig(driverConfig);
                    }

                    // export point
                    List<BatchPoint> points = exportPoint(profile.getId());
                    if (points.size() > 0) {
                        batchProfile.setPoints(points);
                    }

                    // export group
                    List<BatchGroup> groups = exportGroup(
                            profile.getId(),
                            pointAttributeService.selectByDriverId(driverId)
                    );
                    if (groups.size() > 0) {
                        batchProfile.setGroups(groups);
                    }

                    batchProfiles.add(batchProfile);
                });

        return batchProfiles;
    }

    /**
     * 导出 DriverConfig
     *
     * @param driverId  Driver Id
     * @param profileId Profile Id
     * @return Map<String, String> DriverConfig
     */
    private Map<String, String> exportDriverConfig(Long driverId, Long profileId) {
        Map<String, String> driverConfigMap = new HashMap<>(16);

        driverAttributeService.selectByDriverId(driverId)
                .forEach(driverAttribute -> {
                    DriverInfo driverInfo = driverInfoService
                            .selectByAttributeIdAndProfileId(
                                    driverAttribute.getId(),
                                    profileId
                            );
                    driverConfigMap.put(driverAttribute.getName(), driverInfo.getValue());
                });

        return driverConfigMap;
    }

    /**
     * 导出 Point
     *
     * @param profileId Profile Id
     * @return BatchPoint Array
     */
    private List<BatchPoint> exportPoint(Long profileId) {
        List<BatchPoint> batchPoints = new ArrayList<>(16);

        pointService.selectByProfileId(profileId)
                .forEach(point -> {
                    BatchPoint batchPoint = new BatchPoint();
                    BeanUtils.copyProperties(point, batchPoint);
                    batchPoints.add(batchPoint);
                });

        return batchPoints;
    }

    /**
     * 导出 Group
     *
     * @param profileId Profile Id
     * @return BatchGroup Array
     */
    private List<BatchGroup> exportGroup(Long profileId, List<PointAttribute> pointAttributes) {
        List<BatchGroup> batchGroups = new ArrayList<>(16);

        List<Device> devices = deviceService.selectDeviceByProfileId(profileId);
        List<Point> points = pointService.selectByProfileId(profileId);

        devices.stream().map(Device::getGroupId).distinct()
                .forEach(groupId -> {
                    Group group = groupService.selectById(groupId);
                    BatchGroup batchGroup = new BatchGroup();
                    batchGroup.setName(group.getName());

                    // export device
                    List<BatchDevice> batchDevices = exportDevice(
                            groupId,
                            devices,
                            points,
                            pointAttributes
                    );

                    if (batchDevices.size() > 0) {
                        batchGroup.setDevices(batchDevices);
                    }
                    batchGroups.add(batchGroup);
                });

        return batchGroups;
    }

    /**
     * Export device array
     *
     * @param groupId         Group Id
     * @param devices         Device Array
     * @param points          Point Array
     * @param pointAttributes PointAttribute Array
     * @return BatchDevice Array
     */
    private List<BatchDevice> exportDevice(Long groupId, List<Device> devices, List<Point> points, List<PointAttribute> pointAttributes) {
        List<BatchDevice> batchDevices = new ArrayList<>(16);

        devices.stream().filter(device -> device.getGroupId().equals(groupId))
                .forEach(device -> {
                    // export point config
                    Map<String, Map<String, String>> pointConfigMap = new HashMap<>(16);
                    points.forEach(point -> {
                        Map<String, String> configMap = new HashMap<>(8);
                        pointAttributes.forEach(pointAttribute -> {
                            try {
                                PointInfo pointInfo = pointInfoService
                                        .selectByAttributeIdAndDeviceIdAndPointId(
                                                pointAttribute.getId(),
                                                device.getId(),
                                                point.getId()
                                        );
                                configMap.put(pointAttribute.getName(), pointInfo.getValue());
                            } catch (NotFoundException ignored) {
                            }
                        });

                        if (configMap.size() > 0) {
                            pointConfigMap.put(point.getName(), configMap);
                        }
                    });

                    BatchDevice batchDevice = new BatchDevice();
                    batchDevice.setName(device.getName());
                    batchDevice.setMulti(device.getMulti());

                    if (pointConfigMap.size() > 0) {
                        batchDevice.setPointConfig(pointConfigMap);
                    }
                    batchDevices.add(batchDevice);
                });

        return batchDevices;
    }

    /**
     * Get driver attribute map
     *
     * @param driverId Driver Id
     * @return map(driverAttributeId, driverAttribute)
     */
    public Map<Long, DriverAttribute> getDriverAttributeMap(long driverId) {
        Map<Long, DriverAttribute> driverAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverAttribute> driverAttributes = driverAttributeService.selectByDriverId(driverId);
            driverAttributes.forEach(driverAttribute -> driverAttributeMap.put(driverAttribute.getId(), driverAttribute));
        } catch (NotFoundException ignored) {
        }
        return driverAttributeMap;
    }

    /**
     * Get point attribute map
     *
     * @param driverId Driver Id
     * @return map(pointAttributeId, pointAttribute)
     */
    public Map<Long, PointAttribute> getPointAttributeMap(long driverId) {
        Map<Long, PointAttribute> pointAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(driverId);
            pointAttributes.forEach(pointAttribute -> pointAttributeMap.put(pointAttribute.getId(), pointAttribute));
        } catch (NotFoundException ignored) {
        }
        return pointAttributeMap;
    }

    /**
     * Get driver info map
     *
     * @param profileList        Profile Array
     * @param driverAttributeMap Driver Attribute Map
     * @return map(profileId ( driverAttribute.name, ( drverInfo.value, driverAttribute.type)))
     */
    public Map<Long, Map<String, AttributeInfo>> getProfileDriverInfoMap(List<Long> profileList, Map<Long, DriverAttribute> driverAttributeMap) {
        Map<Long, Map<String, AttributeInfo>> driverInfoMap = new ConcurrentHashMap<>(16);
        profileList.forEach(profileId -> {
            Map<String, AttributeInfo> infoMap = getDriverInfoMap(profileId, driverAttributeMap);
            if (infoMap.size() > 0) {
                driverInfoMap.put(profileId, infoMap);
            }
        });
        return driverInfoMap;
    }

    /**
     * Get profile point map
     *
     * @param profileList Profile Array
     * @return map(profileId ( pointId, point))
     */
    public Map<Long, Map<Long, Point>> getProfilePointMap(List<Long> profileList) {
        Map<Long, Map<Long, Point>> pointMap = new ConcurrentHashMap<>(16);
        profileList.forEach(profileId -> pointMap.put(profileId, getPointMap(profileId)));
        return pointMap;
    }

    /**
     * Get driver info map
     *
     * @param profileId          Profile Id
     * @param driverAttributeMap Driver Attribute Map
     * @return map(attributeName, attributeInfo ( value, type))
     */
    public Map<String, AttributeInfo> getDriverInfoMap(Long profileId, Map<Long, DriverAttribute> driverAttributeMap) {
        Map<String, AttributeInfo> attributeInfoMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverInfo> driverInfos = driverInfoService.selectByProfileId(profileId);
            driverInfos.forEach(driverInfo -> {
                DriverAttribute attribute = driverAttributeMap.get(driverInfo.getDriverAttributeId());
                attributeInfoMap.put(attribute.getName(), new AttributeInfo(driverInfo.getValue(), attribute.getType()));
            });
        } catch (NotFoundException ignored) {
        }
        return attributeInfoMap;
    }

    /**
     * Get point info map
     *
     * @param device            Device
     * @param profilePointMap   Profile Point Map
     * @param pointAttributeMap Point Attribute Map
     * @return map(pointId, attribute ( attributeName, attributeInfo ( value, type)))
     */
    public Map<Long, Map<String, AttributeInfo>> getDevicePointInfoMap(Device device, Map<Long, Map<Long, Point>> profilePointMap, Map<Long, PointAttribute> pointAttributeMap) {
        Map<Long, Map<String, AttributeInfo>> attributeInfoMap = new ConcurrentHashMap<>(16);
        profilePointMap.get(device.getProfileId()).keySet()
                .forEach(pointId -> {
                    List<PointInfo> pointInfos = pointInfoService.selectByDeviceIdAndPointId(device.getId(), pointId);
                    Map<String, AttributeInfo> infoMap = new ConcurrentHashMap<>(16);
                    pointInfos.forEach(pointInfo -> {
                        PointAttribute attribute = pointAttributeMap.get(pointInfo.getPointAttributeId());
                        infoMap.put(attribute.getName(), new AttributeInfo(pointInfo.getValue(), attribute.getType()));
                    });

                    if (infoMap.size() > 0) {
                        attributeInfoMap.put(pointId, infoMap);
                    }
                });
        return attributeInfoMap;
    }

    /**
     * Get point name map
     *
     * @param profileId Profile Id
     * @return map(pointName, pointId)
     */
    public Map<String, Long> getPointNameMap(Long profileId) {
        Map<String, Long> pointNameMap = new ConcurrentHashMap<>(16);
        pointService.selectByProfileId(profileId).forEach(point -> pointNameMap.put(point.getName(), point.getId()));
        return pointNameMap;
    }

    /**
     * Get point map
     *
     * @param profileId Profile Id
     * @return map(pointId, point)
     */
    public Map<Long, Point> getPointMap(Long profileId) {
        Map<Long, Point> pointMap = new ConcurrentHashMap<>(16);
        pointService.selectByProfileId(profileId).forEach(point -> pointMap.put(point.getId(), point));
        return pointMap;
    }


}
