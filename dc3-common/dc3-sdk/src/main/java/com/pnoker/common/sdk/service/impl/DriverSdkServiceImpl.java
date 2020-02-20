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
    private DriverClient driverClient;
    @Resource
    private ConnectInfoClient connectInfoClient;
    @Resource
    private ProfileInfoClient profileInfoClient;
    @Resource
    private ProfileClient profileClient;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private PointClient pointClient;
    @Resource
    private PointInfoClient pointInfoClient;
    @Resource
    private DeviceDriver driver;
    @Resource
    private DriverCustomizersService customizersService;

    @Resource
    private DriverProperty driverProperty;

    @Override
    public void initial(ApplicationContext context) {
        if (!register()) {
            ((ConfigurableApplicationContext) context).close();
        }
        loadData();
    }

    @Override
    public void read(Long deviceId, Long pointId) {
        Device device = driver.getDeviceMap().get(deviceId);
        Profile profile = driver.getProfileMap().get(device.getProfileId());
        Point point = driver.getPointMap().get(profile.getId()).get(pointId);

        PointInfoDto pointInfoDto = new PointInfoDto();
        pointInfoDto.setDeviceId(deviceId).setPointId(pointId);
        pointInfoDto.setPage(new Pages().setSize(-1L));

        Map<String, String> pp = new HashMap<>(16);
        R<Page<PointInfo>> r = pointInfoClient.list(pointInfoDto);
        if (r.isOk()) {
            List<PointInfo> pointInfos = r.getData().getRecords();
            for (PointInfo pointInfo : pointInfos) {
                pp.put(driver.getProfileInfoMap().get(pointInfo.getProfileInfoId()).getName(), pointInfo.getValue());
            }
        customizersService.read(pp, pp, point);
        }
    }

    @Override
    public void addDevice(Long id) {
        R<Device> r = deviceClient.selectById(id);
        if (r.isOk()) {
            driver.getDeviceMap().put(r.getData().getId(), r.getData());
        }
    }

    @Override
    public void deleteDevice(Long id) {
        driver.getDeviceMap().entrySet().removeIf(next -> next.getKey().equals(id));
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
            driver.getProfileMap().put(r.getData().getId(), r.getData());
        }
    }

    @Override
    public void deleteProfile(Long id) {
        driver.getProfileMap().entrySet().removeIf(next -> next.getKey().equals(id));
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
        if (!Dc3Util.isDriverPort(driver.getPort())) {
            log.error("invalid driver port,port range is 8600-8799");
            return false;
        }
        if (!Dc3Util.isName(driverProperty.getName()) || !Dc3Util.isName(driver.getServiceName()) || !Dc3Util.isHost(driver.getHost())) {
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
        Driver tmp = new Driver(driverProperty.getName(), driver.getServiceName(), driver.getHost(), driver.getPort());
        tmp.setDescription(driverProperty.getDescription());

        R<Driver> byServiceName = driverClient.selectByServiceName(tmp.getServiceName());
        if (byServiceName.isOk()) {
            tmp.setId(byServiceName.getData().getId());
            driver.setDriverId(tmp.getId());
            return driverClient.update(tmp).isOk();
        } else {
            R<Driver> byHostPort = driverClient.selectByHostPort(driver.getHost(), driver.getPort());
            if (!byHostPort.isOk()) {
                R<Driver> r = driverClient.add(tmp);
                if (r.isOk()) {
                    driver.setDriverId(tmp.getId());
                }
                return r.isOk();
            }
            log.error("the port({}) is already occupied by driver({}/{})", driver.getPort(), byHostPort.getData().getName(), byHostPort.getData().getServiceName());
            return false;
        }
    }

    /**
     * 注册驱动 connect 配置信息
     *
     * @return
     */
    public boolean registerDriverConnectInfo() {
        Map<String, ConnectInfo> infoMap = new HashMap<>(16);
        ConnectInfoDto connectInfoDto = new ConnectInfoDto();
        connectInfoDto.setDriverId(driver.getDriverId());
        connectInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<ConnectInfo>> list = connectInfoClient.list(connectInfoDto);
        if (list.isOk()) {
            for (ConnectInfo info : list.getData().getRecords()) {
                infoMap.put(info.getName(), info);
            }
        }

        Map<String, ConnectInfo> connectInfoMap = new HashMap<>(16);
        for (ConnectInfo info : driverProperty.getConnect()) {
            connectInfoMap.put(info.getName(), info);
        }

        for (String name : connectInfoMap.keySet()) {
            ConnectInfo info = connectInfoMap.get(name).setDriverId(driver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<ConnectInfo> r = connectInfoClient.update(info);
                if (!r.isOk()) {
                    log.error("the connect info ({}) update failed", name);
                    return false;
                }
            } else {
                R<ConnectInfo> r = connectInfoClient.add(info);
                if (!r.isOk()) {
                    log.error("the connect info ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!connectInfoMap.containsKey(name)) {
                R<Boolean> r = connectInfoClient.delete(infoMap.get(name).getId());
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
        Map<String, ProfileInfo> infoMap = new HashMap<>(16);
        ProfileInfoDto profileInfoDto = new ProfileInfoDto();
        profileInfoDto.setDriverId(driver.getDriverId());
        profileInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<ProfileInfo>> list = profileInfoClient.list(profileInfoDto);
        if (list.isOk()) {
            for (ProfileInfo info : list.getData().getRecords()) {
                infoMap.put(info.getName(), info);
            }
        }

        Map<String, ProfileInfo> profileInfoMap = new HashMap<>(16);
        for (ProfileInfo info : driverProperty.getProfile()) {
            profileInfoMap.put(info.getName(), info);
        }

        for (String name : profileInfoMap.keySet()) {
            ProfileInfo info = profileInfoMap.get(name).setDriverId(driver.getDriverId());
            if (infoMap.containsKey(name)) {
                info.setId(infoMap.get(name).getId());
                R<ProfileInfo> r = profileInfoClient.update(info);
                if (!r.isOk()) {
                    log.error("the profile info ({}) update failed", name);
                    return false;
                }
            } else {
                R<ProfileInfo> r = profileInfoClient.add(info);
                if (!r.isOk()) {
                    log.error("the profile info ({}) create failed", name);
                    return false;
                }
            }
        }

        for (String name : infoMap.keySet()) {
            if (!profileInfoMap.containsKey(name)) {
                R<Boolean> r = profileInfoClient.delete(infoMap.get(name).getId());
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
        driver.setConnectInfoMap(getConnectInfoMap(driver.getDriverId()));
        driver.setProfileInfoMap(getProfileInfoMap(driver.getDriverId()));
        driver.setProfileMap(getProfileMap(driver.getDriverId()));
        driver.setDeviceMap(getDeviceMap(driver.getProfileMap()));
        driver.setPointMap(getPointMap(driver.getProfileMap()));
    }

    /**
     * 获取驱动 connect 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, ConnectInfo> getConnectInfoMap(long driverId) {
        Map<Long, ConnectInfo> infoMap = new HashMap<>(16);
        ConnectInfoDto connectInfoDto = new ConnectInfoDto();
        connectInfoDto.setDriverId(driverId);
        connectInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<ConnectInfo>> r = connectInfoClient.list(connectInfoDto);
        if (r.isOk()) {
            for (ConnectInfo info : r.getData().getRecords()) {
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
    public Map<Long, ProfileInfo> getProfileInfoMap(long driverId) {
        Map<Long, ProfileInfo> infoMap = new HashMap<>(16);
        ProfileInfoDto profileInfoDto = new ProfileInfoDto();
        profileInfoDto.setDriverId(driverId);
        profileInfoDto.setPage(new Pages().setSize(-1L));
        R<Page<ProfileInfo>> r = profileInfoClient.list(profileInfoDto);
        if (r.isOk()) {
            for (ProfileInfo info : r.getData().getRecords()) {
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
