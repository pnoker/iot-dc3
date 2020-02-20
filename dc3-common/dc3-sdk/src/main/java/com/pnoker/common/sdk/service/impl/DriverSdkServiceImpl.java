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
import com.pnoker.common.sdk.bean.DriverProperty;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.DriverCustomizersService;
import com.pnoker.common.sdk.service.DriverSdkService;
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
public class DriverSdkServiceImpl implements DriverSdkService {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private DriverClient driverClient;
    @Resource
    private DriverAttributeClient driverAttributeClient;
    @Resource
    private PointAttributeClient pointAttributeClient;
    @Resource
    private ProfileClient profileClient;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private PointClient pointClient;
    @Resource
    private PointInfoClient pointInfoClient;
    @Resource
    private DriverInfoClient driverInfoClient;
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private DriverCustomizersService customizersService;


    @Resource
    private DriverProperty driverProperty;

    @Override
    public void initial() {
        if (!register()) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
        loadData();
    }

    @Override
    public void read(Long deviceId, Long pointId) {
        Device device = deviceDriver.getDeviceMap().get(deviceId);
        Profile profile = deviceDriver.getProfileMap().get(device.getProfileId());
        Point point = deviceDriver.getPointMap().get(profile.getId()).get(pointId);

        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setProfileId(profile.getId());
        driverInfoDto.setPage(new Pages().setSize(-1L));
        Map<String, String> dd = new HashMap<>(16);
        R<Page<DriverInfo>> rd = driverInfoClient.list(driverInfoDto);
        if (rd.isOk()) {
            List<DriverInfo> driverInfos = rd.getData().getRecords();
            for (DriverInfo driverInfo : driverInfos) {
                dd.put(deviceDriver.getDriverAttributeMap().get(driverInfo.getDriverAttributeId()).getName(), driverInfo.getValue());
            }
        }

        PointInfoDto pointInfoDto = new PointInfoDto();
        pointInfoDto.setDeviceId(deviceId).setPointId(pointId);
        pointInfoDto.setPage(new Pages().setSize(-1L));
        Map<String, String> pp = new HashMap<>(16);
        R<Page<PointInfo>> rp = pointInfoClient.list(pointInfoDto);
        if (rp.isOk()) {
            List<PointInfo> pointInfos = rp.getData().getRecords();
            for (PointInfo pointInfo : pointInfos) {
                pp.put(deviceDriver.getPointAttributeMap().get(pointInfo.getPointAttributeId()).getName(), pointInfo.getValue());
            }
        }


        customizersService.read(pp, pp, point);
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
        return registerDriver() && registerDriverConnectInfo() && registerDriverProfileInfo();
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
     * 注册驱动 connect 配置信息
     *
     * @return
     */
    public boolean registerDriverConnectInfo() {
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

        Map<String, DriverAttribute> connectInfoMap = new HashMap<>(16);
        for (DriverAttribute info : driverProperty.getConnect()) {
            connectInfoMap.put(info.getName(), info);
        }

        for (String name : connectInfoMap.keySet()) {
            DriverAttribute info = connectInfoMap.get(name).setDriverId(deviceDriver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<DriverAttribute> r = driverAttributeClient.update(info);
                if (!r.isOk()) {
                    log.error("the connect info ({}) update failed", name);
                    return false;
                }
            } else {
                R<DriverAttribute> r = driverAttributeClient.add(info);
                if (!r.isOk()) {
                    log.error("the connect info ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!connectInfoMap.containsKey(name)) {
                R<Boolean> r = driverAttributeClient.delete(infoMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("the connect info ({}) delete failed", name);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 注册驱动 profile 配置信息
     *
     * @return
     */
    public boolean registerDriverProfileInfo() {
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

        Map<String, PointAttribute> profileInfoMap = new HashMap<>(16);
        for (PointAttribute info : driverProperty.getProfile()) {
            profileInfoMap.put(info.getName(), info);
        }

        for (String name : profileInfoMap.keySet()) {
            PointAttribute info = profileInfoMap.get(name).setDriverId(deviceDriver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<PointAttribute> r = pointAttributeClient.update(info);
                if (!r.isOk()) {
                    log.error("the profile info ({}) update failed", name);
                    return false;
                }
            } else {
                R<PointAttribute> r = pointAttributeClient.add(info);
                if (!r.isOk()) {
                    log.error("the profile info ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!profileInfoMap.containsKey(name)) {
                R<Boolean> r = pointAttributeClient.delete(infoMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("the profile info ({}) delete failed", name);
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
        deviceDriver.setDriverAttributeMap(getConnectInfoMap(deviceDriver.getDriverId()));
        deviceDriver.setPointAttributeMap(getProfileInfoMap(deviceDriver.getDriverId()));
        deviceDriver.setProfileMap(getProfileMap(deviceDriver.getDriverId()));
        deviceDriver.setDeviceMap(getDeviceMap(deviceDriver.getProfileMap()));
        deviceDriver.setPointMap(getPointMap(deviceDriver.getProfileMap()));
    }

    /**
     * 获取驱动 connect 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, DriverAttribute> getConnectInfoMap(long driverId) {
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
    public Map<Long, PointAttribute> getProfileInfoMap(long driverId) {
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

}
