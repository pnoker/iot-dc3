/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.common.sdk.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.feign.*;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.R;
import com.pnoker.common.dto.*;
import com.pnoker.common.model.*;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.bean.DriverProperty;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.SdkService;
import com.pnoker.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
@EnableConfigurationProperties({DriverProperty.class})
public class SdkServiceImpl implements SdkService {
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private DriverAttributeClient driverAttributeClient;
    @Resource
    private PointAttributeClient pointAttributeClient;
    @Resource
    private PointInfoClient pointInfoClient;
    @Resource
    private DriverInfoClient driverInfoClient;
    @Resource
    private DriverClient driverClient;
    @Resource
    private ProfileClient profileClient;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private PointClient pointClient;

    @Override
    public void initial() {
        if (!register()) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
        loadData();
    }

    @Override
    public void addDevice(Long id) {
        R<Device> r = deviceClient.selectById(id);
        if (r.isOk()) {
            deviceDriver.getDeviceMap().put(r.getData().getId(), r.getData());
        }
    }

    @Override
    public void deleteDevice(Long id) {
        deviceDriver.getDeviceMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void updateDevice(Long id) {
        deleteDevice(id);
        addDevice(id);
    }

    @Override
    public void addProfile(Long id) {
        R<Profile> r = profileClient.selectById(id);
        if (r.isOk()) {
            deviceDriver.getProfileMap().put(r.getData().getId(), r.getData());
        }
    }

    @Override
    public void deleteProfile(Long id) {
        deviceDriver.getProfileMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void updateProfile(Long id) {
        deleteProfile(id);
        addProfile(id);
    }

    /**
     * 注册
     *
     * @return
     */
    public boolean register() {
        if (!Dc3Util.isDriverPort(deviceDriver.getPort())) {
            log.error("invalid driver port,port range is 8600-8799");
            return false;
        }
        if (!Dc3Util.isName(driverProperty.getName()) || !Dc3Util.isName(deviceDriver.getServiceName()) || !Dc3Util.isHost(deviceDriver.getHost())) {
            log.error("driver name || driver service name || driver host is invalid");
            return false;
        }
        return registerDriver() && registerDriverAttribute() && registerPointAttribute();
    }

    /**
     * 注册驱动信息
     *
     * @return
     */
    public boolean registerDriver() {
        Driver tmp = new Driver(driverProperty.getName(), deviceDriver.getServiceName(), deviceDriver.getHost(), deviceDriver.getPort());
        tmp.setDescription(driverProperty.getDescription());

        R<Driver> byServiceName = driverClient.selectByServiceName(tmp.getServiceName());
        if (byServiceName.isOk()) {
            tmp.setId(byServiceName.getData().getId());
            deviceDriver.setDriverId(tmp.getId());
            return driverClient.update(tmp).isOk();
        } else {
            R<Driver> byHostPort = driverClient.selectByHostPort(deviceDriver.getHost(), deviceDriver.getPort());
            if (!byHostPort.isOk()) {
                R<Driver> r = driverClient.add(tmp);
                if (r.isOk()) {
                    deviceDriver.setDriverId(tmp.getId());
                }
                return r.isOk();
            }
            log.error("the port({}) is already occupied by driver({}/{})", deviceDriver.getPort(), byHostPort.getData().getName(), byHostPort.getData().getServiceName());
            return false;
        }
    }

    /**
     * 注册驱动 driver 配置属性
     *
     * @return
     */
    public boolean registerDriverAttribute() {
        Map<String, DriverAttribute> infoMap = new HashMap<>(16);
        DriverAttributeDto connectInfoDto = new DriverAttributeDto();
        connectInfoDto.setDriverId(deviceDriver.getDriverId());
        connectInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<DriverAttribute>> list = driverAttributeClient.list(connectInfoDto);
        if (list.isOk()) {
            for (DriverAttribute info : list.getData().getRecords()) {
                infoMap.put(info.getName(), info);
            }
        }

        Map<String, DriverAttribute> driverAttributeMap = new HashMap<>(16);
        for (DriverAttribute info : driverProperty.getDriverAttribute()) {
            driverAttributeMap.put(info.getName(), info);
        }

        for (String name : driverAttributeMap.keySet()) {
            DriverAttribute info = driverAttributeMap.get(name).setDriverId(deviceDriver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<DriverAttribute> r = driverAttributeClient.update(info);
                if (!r.isOk()) {
                    log.error("the driver attribute ({}) update failed", name);
                    return false;
                }
            } else {
                R<DriverAttribute> r = driverAttributeClient.add(info);
                if (!r.isOk()) {
                    log.error("the driver attribute ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!driverAttributeMap.containsKey(name)) {
                R<Boolean> r = driverAttributeClient.delete(infoMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("the driver attribute ({}) delete failed", name);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 注册驱动 point 配置属性
     *
     * @return
     */
    public boolean registerPointAttribute() {
        Map<String, PointAttribute> infoMap = new HashMap<>(16);
        PointAttributeDto profileInfoDto = new PointAttributeDto();
        profileInfoDto.setDriverId(deviceDriver.getDriverId());
        profileInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<PointAttribute>> list = pointAttributeClient.list(profileInfoDto);
        if (list.isOk()) {
            for (PointAttribute info : list.getData().getRecords()) {
                infoMap.put(info.getName(), info);
            }
        }

        Map<String, PointAttribute> pointAttributeMap = new HashMap<>(16);
        for (PointAttribute info : driverProperty.getPointAttribute()) {
            pointAttributeMap.put(info.getName(), info);
        }

        for (String name : pointAttributeMap.keySet()) {
            PointAttribute info = pointAttributeMap.get(name).setDriverId(deviceDriver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<PointAttribute> r = pointAttributeClient.update(info);
                if (!r.isOk()) {
                    log.error("the point attribute ({}) update failed", name);
                    return false;
                }
            } else {
                R<PointAttribute> r = pointAttributeClient.add(info);
                if (!r.isOk()) {
                    log.error("the point attribute ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!pointAttributeMap.containsKey(name)) {
                R<Boolean> r = pointAttributeClient.delete(infoMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("the point attribute ({}) delete failed", name);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 加载数据
     */
    public void loadData() {
        deviceDriver.setProfileMap(getProfileMap(deviceDriver.getDriverId()));
        deviceDriver.setDriverInfoMap(getDriverInfoMap(deviceDriver.getProfileMap(), getDriverAttributeMap(deviceDriver.getDriverId())));
        deviceDriver.setDeviceMap(getDeviceMap(deviceDriver.getProfileMap()));
        deviceDriver.setPointMap(getPointMap(deviceDriver.getProfileMap()));
        deviceDriver.setPointInfoMap(getPointInfoMap(deviceDriver.getDeviceMap(), getPointAttributeMap(deviceDriver.getDriverId())));
    }

    /**
     * 获取驱动 connect 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, DriverAttribute> getDriverAttributeMap(long driverId) {
        Map<Long, DriverAttribute> infoMap = new HashMap<>(16);
        DriverAttributeDto connectInfoDto = new DriverAttributeDto();
        connectInfoDto.setDriverId(driverId);
        connectInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<DriverAttribute>> r = driverAttributeClient.list(connectInfoDto);
        if (r.isOk()) {
            for (DriverAttribute info : r.getData().getRecords()) {
                infoMap.put(info.getId(), info);
            }
        }
        return infoMap;
    }

    /**
     * 获取驱动 profile 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, PointAttribute> getPointAttributeMap(long driverId) {
        Map<Long, PointAttribute> infoMap = new HashMap<>(16);
        PointAttributeDto profileInfoDto = new PointAttributeDto();
        profileInfoDto.setDriverId(driverId);
        profileInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<PointAttribute>> r = pointAttributeClient.list(profileInfoDto);
        if (r.isOk()) {
            for (PointAttribute info : r.getData().getRecords()) {
                infoMap.put(info.getId(), info);
            }
        }
        return infoMap;
    }

    /**
     * 获取模板
     *
     * @param driverId
     * @return
     */
    public Map<Long, Profile> getProfileMap(long driverId) {
        Map<Long, Profile> profileMap = new HashMap<>(16);
        ProfileDto profileDto = new ProfileDto();
        profileDto.setDriverId(driverId);
        profileDto.setPage(new Pages().setSize(-1L));
        R<Page<Profile>> r = profileClient.list(profileDto);
        if (r.isOk()) {
            for (Profile profile : r.getData().getRecords()) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    /**
     * 获取设备
     *
     * @param profileMap
     * @return
     */
    public Map<Long, Device> getDeviceMap(Map<Long, Profile> profileMap) {
        Map<Long, Device> deviceMap = new HashMap<>(16);
        for (Long profileId : profileMap.keySet()) {
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setProfileId(profileId);
            deviceDto.setPage(new Pages().setSize(-1L));
            R<Page<Device>> r = deviceClient.list(deviceDto);
            if (r.isOk()) {
                for (Device device : r.getData().getRecords()) {
                    deviceMap.put(device.getId(), device);
                }
            }
        }
        return deviceMap;
    }

    /**
     * 获取位号
     * profileId(pointId,point)
     *
     * @param profileMap
     * @return
     */
    public Map<Long, Map<Long, Point>> getPointMap(Map<Long, Profile> profileMap) {
        Map<Long, Map<Long, Point>> pointMap = new HashMap<>(16);
        for (Long profileId : profileMap.keySet()) {
            PointDto pointDto = new PointDto();
            pointDto.setProfileId(profileId);
            pointDto.setPage(new Pages().setSize(-1L));
            R<Page<Point>> r = pointClient.list(pointDto);
            if (r.isOk()) {
                Map<Long, Point> tmpMap = new HashMap<>(16);
                for (Point point : r.getData().getRecords()) {
                    tmpMap.put(point.getId(), point);
                }
                pointMap.put(profileId, tmpMap);
            }
        }
        return pointMap;
    }

    /**
     * 获取驱动信息
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     *
     * @param profileMap
     * @return
     */
    public Map<Long, Map<String, AttributeInfo>> getDriverInfoMap(Map<Long, Profile> profileMap, Map<Long, DriverAttribute> driverAttributeMap) {
        Map<Long, Map<String, AttributeInfo>> driverInfoMap = new HashMap<>(16);
        for (Long profileId : profileMap.keySet()) {
            DriverInfoDto driverInfoDto = new DriverInfoDto();
            driverInfoDto.setProfileId(profileId);
            driverInfoDto.setPage(new Pages().setSize(-1L));
            R<Page<DriverInfo>> r = driverInfoClient.list(driverInfoDto);
            if (r.isOk()) {
                Map<String, AttributeInfo> infoMap = new HashMap<>(16);
                List<DriverInfo> driverInfos = r.getData().getRecords();
                for (DriverInfo driverInfo : driverInfos) {
                    DriverAttribute attribute = driverAttributeMap.get(driverInfo.getDriverAttributeId());
                    infoMap.put(attribute.getName(), new AttributeInfo(driverInfo.getValue(), attribute.getType()));
                }
                driverInfoMap.put(profileId, infoMap);
            }
        }
        return driverInfoMap;
    }

    /**
     * 获取位号信息
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     *
     * @return
     */
    public Map<Long, Map<Long, Map<String, AttributeInfo>>> getPointInfoMap(Map<Long, Device> deviceMap, Map<Long, PointAttribute> pointAttributeMap) {
        Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap = new HashMap<>(16);
        for (Device device : deviceMap.values()) {
            Map<Long, Map<String, AttributeInfo>> tmp = new HashMap<>(16);
            Profile profile = deviceDriver.getProfileMap().get(device.getProfileId());
            Map<Long, Point> pointMap = deviceDriver.getPointMap().get(profile.getId());
            for (Long pointId : pointMap.keySet()) {
                PointInfoDto pointInfoDto = new PointInfoDto();
                pointInfoDto.setDeviceId(device.getId()).setPointId(pointId);
                pointInfoDto.setPage(new Pages().setSize(-1L));
                R<Page<PointInfo>> r = pointInfoClient.list(pointInfoDto);
                if (r.isOk()) {
                    Map<String, AttributeInfo> infoMap = new HashMap<>(16);
                    List<PointInfo> pointInfos = r.getData().getRecords();
                    for (PointInfo pointInfo : pointInfos) {
                        PointAttribute attribute = pointAttributeMap.get(pointInfo.getPointAttributeId());
                        infoMap.put(attribute.getName(), new AttributeInfo(pointInfo.getValue(), attribute.getType()));
                    }
                    tmp.put(pointId, infoMap);
                }
            }
            pointInfoMap.put(device.getId(), tmp);
        }
        return pointInfoMap;
    }

}
