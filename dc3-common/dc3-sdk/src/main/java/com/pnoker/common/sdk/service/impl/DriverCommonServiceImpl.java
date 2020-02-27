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
import com.pnoker.common.sdk.bean.DriverContext;
import com.pnoker.common.sdk.bean.DriverProperty;
import com.pnoker.common.sdk.service.DriverCommonService;
import com.pnoker.common.sdk.service.DriverScheduleService;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
@EnableConfigurationProperties({DriverProperty.class})
public class DriverCommonServiceImpl implements DriverCommonService {
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${server.port}")
    private int port;

    private Map<Long, DriverAttribute> driverAttributeMap;
    private Map<Long, PointAttribute> pointAttributeMap;

    @Resource
    private DriverScheduleService driverScheduleService;
    @Resource
    private DriverService driverService;

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverProperty driverProperty;
    @Resource
    private DriverClient driverClient;
    @Resource
    private DriverAttributeClient driverAttributeClient;
    @Resource
    private PointAttributeClient pointAttributeClient;
    @Resource
    private ProfileClient profileClient;
    @Resource
    private DriverInfoClient driverInfoClient;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private PointClient pointClient;
    @Resource
    private PointInfoClient pointInfoClient;

    @Override
    public void initial() {
        if (!register()) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
        loadData();
        driverService.initial();
        driverScheduleService.initial(driverProperty.getSchedule());
    }

    @Override
    public void addDevice(Long id) {
        R<Device> r = deviceClient.selectById(id);
        if (r.isOk()) {
            driverContext.getDeviceIdMap().put(r.getData().getId(), r.getData());
            driverContext.getDeviceNameMap().put(r.getData().getName(), r.getData().getId());
            driverContext.getPointInfoMap().put(r.getData().getId(), getPointAttributeInfoByDevice(r.getData()));
        }
    }

    @Override
    public void deleteDevice(Long id) {
        driverContext.getDeviceIdMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDeviceNameMap().entrySet().removeIf(next -> next.getValue().equals(id));
        driverContext.getPointInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
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
            driverContext.getDriverInfoMap().put(r.getData().getId(), getDriverAttributeInfoByProfile(r.getData().getId()));
            driverContext.getPointMap().put(r.getData().getId(), getPointMapByProfile(r.getData().getId()));
        }
    }

    @Override
    public void deleteProfile(Long id) {
        driverContext.getDriverInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getPointMap().entrySet().removeIf(next -> next.getKey().equals(id));
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
        if (!Dc3Util.isDriverPort(this.port)) {
            log.error("invalid driver port,port range is 8600-8799");
            return false;
        }
        if (!Dc3Util.isName(driverProperty.getName()) || !Dc3Util.isName(this.serviceName) || !Dc3Util.isHost(getHost())) {
            log.error("driver name || driver service name || driver host is invalid");
            return false;
        }
        return registerDriver() && registerDriverAttribute() && registerPointAttribute();
    }

    public String getHost() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 注册驱动信息
     *
     * @return
     */
    public boolean registerDriver() {
        Driver tmp = new Driver(driverProperty.getName(), this.serviceName, getHost(), this.port);
        tmp.setDescription(driverProperty.getDescription());

        R<Driver> byServiceName = driverClient.selectByServiceName(tmp.getServiceName());
        if (byServiceName.isOk()) {
            tmp.setId(byServiceName.getData().getId());
            driverContext.setDriverId(tmp.getId());
            return driverClient.update(tmp).isOk();
        } else {
            R<Driver> byHostPort = driverClient.selectByHostPort(getHost(), this.port);
            if (!byHostPort.isOk()) {
                R<Driver> r = driverClient.add(tmp);
                if (r.isOk()) {
                    driverContext.setDriverId(tmp.getId());
                }
                return r.isOk();
            }
            log.error("the port({}) is already occupied by driver({}/{})", this.port, byHostPort.getData().getName(), byHostPort.getData().getServiceName());
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
        connectInfoDto.setPage(new Pages().setSize(-1L)).setDriverId(driverContext.getDriverId());
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
            DriverAttribute info = driverAttributeMap.get(name).setDriverId(driverContext.getDriverId());
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
                DriverInfoDto driverInfoDto = new DriverInfoDto();
                driverInfoDto.setPage(new Pages().setSize(-1L)).setDriverAttributeId(infoMap.get(name).getId());
                R<Page<DriverInfo>> tmp = driverInfoClient.list(driverInfoDto);
                if (tmp.isOk() && tmp.getData().getTotal() > 0) {
                    log.error("the driver attribute ({}) used by driver info", name);
                    return false;
                }
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
        PointAttributeDto pointAttributeDto = new PointAttributeDto();
        pointAttributeDto.setPage(new Pages().setSize(-1L)).setDriverId(driverContext.getDriverId());
        R<Page<PointAttribute>> list = pointAttributeClient.list(pointAttributeDto);
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
            PointAttribute info = pointAttributeMap.get(name).setDriverId(driverContext.getDriverId());
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
                PointInfoDto pointInfoDto = new PointInfoDto();
                pointInfoDto.setPage(new Pages().setSize(-1L)).setPointAttributeId(infoMap.get(name).getId());
                R<Page<PointInfo>> tmp = pointInfoClient.list(pointInfoDto);
                if (tmp.isOk() && tmp.getData().getTotal() > 0) {
                    log.error("the point attribute ({}) used by point info", name);
                    return false;
                }
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
        log.info("driver initial basic data ……");
        List<Long> profileList = getProfileList(driverContext.getDriverId());
        this.driverAttributeMap = getDriverAttributeMap(driverContext.getDriverId());
        driverContext.setDriverInfoMap(getDriverInfoMap(profileList, this.driverAttributeMap));
        getDeviceMap(profileList);
        driverContext.setPointMap(getPointMap(profileList));
        this.pointAttributeMap = getPointAttributeMap(driverContext.getDriverId());
        driverContext.setPointInfoMap(getPointInfoMap(driverContext.getDeviceIdMap(), this.pointAttributeMap));
        log.info("driver initial basic data is complete");
    }

    /**
     * 获取驱动 driver 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, DriverAttribute> getDriverAttributeMap(long driverId) {
        Map<Long, DriverAttribute> infoMap = new HashMap<>(16);
        DriverAttributeDto connectInfoDto = new DriverAttributeDto();
        connectInfoDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<DriverAttribute>> rp = driverAttributeClient.list(connectInfoDto);
        if (rp.isOk()) {
            for (DriverAttribute info : rp.getData().getRecords()) {
                infoMap.put(info.getId(), info);
            }
        }
        return infoMap;
    }

    /**
     * 获取驱动 point 配置
     *
     * @param driverId
     * @return
     */
    public Map<Long, PointAttribute> getPointAttributeMap(long driverId) {
        Map<Long, PointAttribute> infoMap = new HashMap<>(16);
        PointAttributeDto pointAttributeDto = new PointAttributeDto();
        pointAttributeDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<PointAttribute>> rp = pointAttributeClient.list(pointAttributeDto);
        if (rp.isOk()) {
            for (PointAttribute info : rp.getData().getRecords()) {
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
    public List<Long> getProfileList(long driverId) {
        List<Long> profileList = new ArrayList<>();
        ProfileDto profileDto = new ProfileDto();
        profileDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<Profile>> rp = profileClient.list(profileDto);
        if (rp.isOk()) {
            for (Profile profile : rp.getData().getRecords()) {
                profileList.add(profile.getId());
            }
        }
        return profileList;
    }

    /**
     * 获取驱动信息
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     *
     * @param profileList
     * @return
     */
    public Map<Long, Map<String, AttributeInfo>> getDriverInfoMap(List<Long> profileList, Map<Long, DriverAttribute> driverAttributeMap) {
        Map<Long, Map<String, AttributeInfo>> driverInfoMap = new HashMap<>(16);
        for (Long profileId : profileList) {
            driverInfoMap.put(profileId, getDriverAttributeInfoByProfile(profileId));
        }
        return driverInfoMap;
    }

    /**
     * 获取设备
     *
     * @param profileList
     * @return
     */
    public void getDeviceMap(List<Long> profileList) {
        driverContext.setDeviceIdMap(new HashMap<>(16));
        driverContext.setDeviceNameMap(new HashMap<>(16));
        for (Long profileId : profileList) {
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
            R<Page<Device>> rp = deviceClient.list(deviceDto);
            if (rp.isOk()) {
                for (Device device : rp.getData().getRecords()) {
                    driverContext.getDeviceIdMap().put(device.getId(), device);
                    driverContext.getDeviceNameMap().put(device.getName(), device.getId());
                }
            }
        }
    }

    /**
     * 获取位号
     * profileId(pointId,point)
     *
     * @param profileList
     * @return
     */
    public Map<Long, Map<Long, Point>> getPointMap(List<Long> profileList) {
        Map<Long, Map<Long, Point>> pointMap = new HashMap<>(16);
        for (Long profileId : profileList) {
            pointMap.put(profileId, getPointMapByProfile(profileId));
        }
        return pointMap;
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
            pointInfoMap.put(device.getId(), getPointAttributeInfoByDevice(device));
        }
        return pointInfoMap;
    }

    public Map<Long, Point> getPointMapByProfile(Long profileId) {
        Map<Long, Point> pointMap = new HashMap<>(16);
        PointDto pointDto = new PointDto();
        pointDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
        R<Page<Point>> rp = pointClient.list(pointDto);
        if (rp.isOk()) {
            for (Point point : rp.getData().getRecords()) {
                pointMap.put(point.getId(), point);
            }
        }
        return pointMap;
    }

    public Map<String, AttributeInfo> getDriverAttributeInfoByProfile(Long profileId) {
        Map<String, AttributeInfo> attributeInfoMap = new HashMap<>(16);
        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
        R<Page<DriverInfo>> rp = driverInfoClient.list(driverInfoDto);
        if (rp.isOk()) {
            for (DriverInfo driverInfo : rp.getData().getRecords()) {
                DriverAttribute attribute = this.driverAttributeMap.get(driverInfo.getDriverAttributeId());
                attributeInfoMap.put(attribute.getName(), new AttributeInfo(driverInfo.getValue(), attribute.getType()));
            }
        }
        return attributeInfoMap;
    }

    public Map<Long, Map<String, AttributeInfo>> getPointAttributeInfoByDevice(Device device) {
        Map<Long, Map<String, AttributeInfo>> attributeInfoMap = new HashMap<>(16);
        Map<Long, Point> pointMap = driverContext.getPointMap().get(device.getProfileId());
        for (Long pointId : pointMap.keySet()) {
            PointInfoDto pointInfoDto = new PointInfoDto();
            pointInfoDto.setPage(new Pages().setSize(-1L)).setDeviceId(device.getId()).setPointId(pointId);
            R<Page<PointInfo>> rp = pointInfoClient.list(pointInfoDto);
            if (rp.isOk()) {
                Map<String, AttributeInfo> infoMap = new HashMap<>(16);
                List<PointInfo> pointInfos = rp.getData().getRecords();
                for (PointInfo pointInfo : pointInfos) {
                    PointAttribute attribute = this.pointAttributeMap.get(pointInfo.getPointAttributeId());
                    infoMap.put(attribute.getName(), new AttributeInfo(pointInfo.getValue(), attribute.getType()));
                }
                attributeInfoMap.put(pointId, infoMap);
            }
        }
        return attributeInfoMap;
    }
}
