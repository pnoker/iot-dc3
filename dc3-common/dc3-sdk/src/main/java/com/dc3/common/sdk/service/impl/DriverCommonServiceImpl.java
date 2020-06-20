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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.*;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.dto.*;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.*;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.bean.DriverProperty;
import com.dc3.common.sdk.service.DriverCommonService;
import com.dc3.common.sdk.service.DriverScheduleService;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private String localHost;

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
        int times = 0;
        boolean reRegister = true;
        while (reRegister) {
            if (!register()) {
                ThreadUtil.sleep(5, TimeUnit.SECONDS);
                times++;
                if (times > 3) {
                    close();
                    throw new ServiceException("Driver registration failed");
                }
            } else {
                reRegister = false;
            }
        }
        log.info("Driver registered successfully");
        loadData();
        driverService.initial();
        driverScheduleService.initial(driverProperty.getSchedule());
    }

    @Override
    public void addProfile(Long id) {
        R<Profile> r = profileClient.selectById(id);
        if (r.isOk()) {
            Map<String, AttributeInfo> infoMap = getProfileDriverInfoMap(r.getData().getId());
            driverContext.getDriverInfoMap().put(r.getData().getId(), infoMap);
            driverContext.getProfilePointMap().put(r.getData().getId(), getPointMap(r.getData().getId()));
        } else {
            log.error("Add profile failed {}", r.getMessage());
        }
    }

    @Override
    public void deleteProfile(Long id) {
        driverContext.getDriverInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getProfilePointMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void addDevice(Long id) {
        R<Device> r = deviceClient.selectById(id);
        if (r.isOk()) {
            driverContext.getDeviceMap().put(r.getData().getId(), r.getData());
            driverContext.getDeviceNameMap().put(r.getData().getName(), r.getData().getId());
            Map<Long, Map<String, AttributeInfo>> infoMap = getDevicePointInfoMap(r.getData());
            driverContext.getDevicePointInfoMap().put(r.getData().getId(), infoMap);
        } else {
            log.error("Add device failed {}", r.getMessage());
        }
    }

    @Override
    public void deleteDevice(Long id) {
        driverContext.getDeviceMap().entrySet().removeIf(next -> next.getKey().equals(id));
        driverContext.getDeviceNameMap().entrySet().removeIf(next -> next.getValue().equals(id));
        driverContext.getDevicePointInfoMap().entrySet().removeIf(next -> next.getKey().equals(id));
    }

    @Override
    public void updateDevice(Long id) {
        addDevice(id);
    }

    @Override
    public void addPoint(Long pointId) {
        R<Point> rp = pointClient.selectById(pointId);
        if (rp.isOk()) {
            Point point = rp.getData();
            driverContext.getProfilePointMap().get(point.getProfileId()).put(point.getId(), point);
        } else {
            log.error("Add point failed {}", rp.getMessage());
        }
    }

    @Override
    public void deletePoint(Long pointId, Long profileId) {
        driverContext.getProfilePointMap().get(profileId).entrySet().removeIf(next -> next.getKey().equals(pointId));
    }

    @Override
    public void updatePoint(Long id) {
        addPoint(id);
    }

    @Override
    public void addDriverInfo(Long id) {
        R<DriverInfo> rd = driverInfoClient.selectById(id);
        if (rd.isOk()) {
            DriverInfo info = rd.getData();
            DriverAttribute attribute = this.driverAttributeMap.get(info.getDriverAttributeId());
            driverContext.getDriverInfoMap().get(info.getProfileId()).put(attribute.getName(), new AttributeInfo(info.getValue(), attribute.getType()));
        } else {
            log.error("Add driver info failed {}", rd.getMessage());
        }
    }

    @Override
    public void deleteDriverInfo(Long attributeId, Long profileId) {
        String attributeName = this.driverAttributeMap.get(attributeId).getName();
        driverContext.getDriverInfoMap().get(profileId).entrySet().removeIf(next -> next.getKey().equals(attributeName));
    }

    @Override
    public void updateDriverInfo(Long id) {
        addDriverInfo(id);
    }

    @Override
    public void addPointInfo(Long id) {
        R<PointInfo> rp = pointInfoClient.selectById(id);
        if (rp.isOk()) {
            PointInfo info = rp.getData();
            PointAttribute attribute = this.pointAttributeMap.get(info.getPointAttributeId());
            if (null == driverContext.getDevicePointInfoMap().get(info.getDeviceId())) {
                driverContext.getDevicePointInfoMap().put(info.getDeviceId(), new ConcurrentHashMap<>(16));
            }
            Map<Long, Map<String, AttributeInfo>> map = driverContext.getDevicePointInfoMap().get(info.getDeviceId());
            if (null == map.get(info.getPointId())) {
                map.put(info.getPointId(), new ConcurrentHashMap<>(16));
            }
            map.get(info.getPointId()).put(attribute.getName(), new AttributeInfo(info.getValue(), attribute.getType()));
        } else {
            log.error("Add point info failed {}", rp.getMessage());
        }
    }

    @Override
    public void deletePointInfo(Long pointId, Long attributeId, Long deviceId) {
        String attributeName = this.pointAttributeMap.get(attributeId).getName();
        driverContext.getDevicePointInfoMap().get(deviceId).get(pointId).entrySet().removeIf(next -> next.getKey().equals(attributeName));
        driverContext.getDevicePointInfoMap().get(deviceId).entrySet().removeIf(next -> next.getValue().size() < 1);
    }

    @Override
    public void updatePointInfo(Long id) {
        addPointInfo(id);
    }

    /**
     * register driver
     *
     * @return boolean
     */
    public boolean register() {
        log.info("Registering {} ", driverProperty.getName());
        if (!Dc3Util.isDriverPort(this.port)) {
            log.error("Invalid driver port, port range is 8600-8799");
            return false;
        }
        this.localHost = Dc3Util.localHost();
        if (!Dc3Util.isName(driverProperty.getName()) || !Dc3Util.isName(this.serviceName) || !Dc3Util.isHost(this.localHost)) {
            log.error("Driver Name || Driver Service Name || Driver Host is invalid");
            return false;
        }
        return registerDriver() && registerDriverAttribute() && registerPointAttribute();
    }

    /**
     * register driver
     * <p>
     * 驱动名称可以重复，但是驱动服务名称，主机IP和端口号不能重复
     *
     * @return boolean
     */
    public boolean registerDriver() {
        Driver driver = new Driver(driverProperty.getName(), this.serviceName, this.localHost, this.port);
        driver.setDescription(driverProperty.getDescription());

        R<Driver> rDriver = driverClient.selectByServiceName(this.serviceName);
        if (rDriver.isOk()) {
            if (null != rDriver.getData()) {
                if (!driverProperty.getName().equals(rDriver.getData().getName())) {
                    log.error("The driver conflicts with driver({}/{})", rDriver.getData().getName(), rDriver.getData().getServiceName());
                    return false;
                }
                driver.setId(rDriver.getData().getId());
                driverContext.setDriverId(driver.getId());
                return driverClient.update(driver).isOk();
            } else {
                R<Driver> byHostPort = driverClient.selectByHostPort(this.localHost, this.port);
                if (byHostPort.isOk()) {
                    if (null != byHostPort.getData()) {
                        log.error("The port({}) is already occupied by driver({}/{})", this.port, byHostPort.getData().getName(), byHostPort.getData().getServiceName());
                        return false;
                    }
                    R<Driver> r = driverClient.add(driver);
                    if (r.isOk()) {
                        driverContext.setDriverId(r.getData().getId());
                    }
                    return r.isOk();
                }
            }
        }
        return false;
    }

    /**
     * register driver attribute
     * <p>
     * 同步数据库和配置文件中的属性配置
     *
     * @return boolean
     */
    public boolean registerDriverAttribute() {
        Map<String, DriverAttribute> oldDriverAttributeMap = new ConcurrentHashMap<>(16);
        DriverAttributeDto connectInfoDto = new DriverAttributeDto();
        connectInfoDto.setPage(new Pages().setSize(-1L)).setDriverId(driverContext.getDriverId());
        R<Page<DriverAttribute>> rDriverAttributes = driverAttributeClient.list(connectInfoDto);
        if (rDriverAttributes.isOk()) {
            rDriverAttributes.getData().getRecords().forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        }

        Map<String, DriverAttribute> newDriverAttributeMap = new ConcurrentHashMap<>(16);
        Optional.ofNullable(driverProperty.getDriverAttribute()).ifPresent(driverAttributes -> {
            driverAttributes.forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        });

        for (String name : newDriverAttributeMap.keySet()) {
            DriverAttribute info = newDriverAttributeMap.get(name).setDriverId(driverContext.getDriverId());
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                R<DriverAttribute> r = driverAttributeClient.update(info);
                if (!r.isOk()) {
                    log.error("The driver attribute({}) update failed", name);
                    return false;
                }
            } else {
                R<DriverAttribute> r = driverAttributeClient.add(info);
                if (!r.isOk()) {
                    log.error("The driver attribute({}) add failed", name);
                    return false;
                }
            }
        }

        for (String name : oldDriverAttributeMap.keySet()) {
            if (!newDriverAttributeMap.containsKey(name)) {
                DriverInfoDto driverInfoDto = new DriverInfoDto();
                driverInfoDto.setPage(new Pages()).setDriverAttributeId(oldDriverAttributeMap.get(name).getId());
                R<Page<DriverInfo>> rDriverInfo = driverInfoClient.list(driverInfoDto);
                if (rDriverInfo.isOk() && rDriverInfo.getData().getRecords().size() > 0) {
                    log.error("The driver attribute({}) used by driver info and cannot be deleted", name);
                    return false;
                }
                R<Boolean> r = driverAttributeClient.delete(oldDriverAttributeMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("The driver attribute({}) delete failed", name);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * register point attribute
     * <p>
     * 同步数据库和配置文件中的属性配置
     *
     * @return boolean
     */
    public boolean registerPointAttribute() {
        Map<String, PointAttribute> oldPointAttributeMap = new ConcurrentHashMap<>(16);
        PointAttributeDto pointAttributeDto = new PointAttributeDto();
        pointAttributeDto.setPage(new Pages().setSize(-1L)).setDriverId(driverContext.getDriverId());
        R<Page<PointAttribute>> list = pointAttributeClient.list(pointAttributeDto);
        if (list.isOk()) {
            list.getData().getRecords().forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        }

        Map<String, PointAttribute> newPointAttributeMap = new ConcurrentHashMap<>(16);
        Optional.ofNullable(driverProperty.getPointAttribute()).ifPresent(pointAttributes -> {
            pointAttributes.forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        });

        for (String name : newPointAttributeMap.keySet()) {
            PointAttribute attribute = newPointAttributeMap.get(name).setDriverId(driverContext.getDriverId());
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                R<PointAttribute> r = pointAttributeClient.update(attribute);
                if (!r.isOk()) {
                    log.error("The point attribute ({}) update failed", name);
                    return false;
                }
            } else {
                R<PointAttribute> r = pointAttributeClient.add(attribute);
                if (!r.isOk()) {
                    log.error("The point attribute ({}) add failed", name);
                    return false;
                }
            }
        }

        for (String name : oldPointAttributeMap.keySet()) {
            if (!newPointAttributeMap.containsKey(name)) {
                PointInfoDto pointInfoDto = new PointInfoDto();
                pointInfoDto.setPage(new Pages()).setPointAttributeId(oldPointAttributeMap.get(name).getId());
                R<Page<PointInfo>> rPointInfo = pointInfoClient.list(pointInfoDto);
                if (rPointInfo.isOk() && rPointInfo.getData().getRecords().size() > 0) {
                    log.error("The point attribute({}) used by point info and cannot be deleted", name);
                    return false;
                }
                R<Boolean> r = pointAttributeClient.delete(oldPointAttributeMap.get(name).getId());
                if (!r.isOk()) {
                    log.error("The point attribute ({}) delete failed", name);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * load data
     */
    public void loadData() {
        this.driverAttributeMap = loadDriverAttributeMap(driverContext.getDriverId());
        this.pointAttributeMap = loadPointAttributeMap(driverContext.getDriverId());

        List<Long> profileList = loadProfile(driverContext.getDriverId());
        driverContext.setDriverInfoMap(loadDriverInfoMap(profileList));
        loadDevice(profileList);
        driverContext.setProfilePointMap(loadProfilePointMap(profileList));
        loadPoint(driverContext.getDeviceMap());
    }

    /**
     * 获取驱动配置属性 Map
     * driverAttributeId,driverAttribute
     *
     * @param driverId Driver Id
     * @return Map
     */
    public Map<Long, DriverAttribute> loadDriverAttributeMap(long driverId) {
        Map<Long, DriverAttribute> driverAttributeMap = new HashMap<>(16);
        DriverAttributeDto driverAttributeDto = new DriverAttributeDto();
        driverAttributeDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<DriverAttribute>> rDriverAttribute = driverAttributeClient.list(driverAttributeDto);
        if (!rDriverAttribute.isOk()) {
            close();
            throw new ServiceException(rDriverAttribute.getMessage());
        }
        rDriverAttribute.getData().getRecords().forEach(driverAttribute -> driverAttributeMap.put(driverAttribute.getId(), driverAttribute));
        return driverAttributeMap;
    }

    /**
     * 获取位号配置属性 Map
     * pointAttributeId,pointAttribute
     *
     * @param driverId Driver Id
     * @return Map
     */
    public Map<Long, PointAttribute> loadPointAttributeMap(long driverId) {
        Map<Long, PointAttribute> pointAttributeMap = new HashMap<>(16);
        PointAttributeDto pointAttributeDto = new PointAttributeDto();
        pointAttributeDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<PointAttribute>> rPointAttribute = pointAttributeClient.list(pointAttributeDto);
        if (!rPointAttribute.isOk()) {
            close();
            throw new ServiceException(rPointAttribute.getMessage());
        }
        rPointAttribute.getData().getRecords().forEach(pointAttribute -> pointAttributeMap.put(pointAttribute.getId(), pointAttribute));
        return pointAttributeMap;
    }

    /**
     * 获取模板驱动配置信息 Map
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     *
     * @param profileList Profile Array
     * @return Map
     */
    public Map<Long, Map<String, AttributeInfo>> loadDriverInfoMap(List<Long> profileList) {
        Map<Long, Map<String, AttributeInfo>> driverInfoMap = new ConcurrentHashMap<>(16);
        for (Long profileId : profileList) {
            Map<String, AttributeInfo> infoMap = getProfileDriverInfoMap(profileId);
            if (infoMap.size() > 0) {
                driverInfoMap.put(profileId, infoMap);
            }
        }
        return driverInfoMap;
    }

    /**
     * 获取模板位号 Map
     * profileId(pointId,point)
     *
     * @param profileList Profile Array
     * @return Map
     */
    public Map<Long, Map<Long, Point>> loadProfilePointMap(List<Long> profileList) {
        Map<Long, Map<Long, Point>> pointMap = new ConcurrentHashMap<>(16);
        for (Long profileId : profileList) {
            pointMap.put(profileId, getPointMap(profileId));
        }
        return pointMap;
    }

    /**
     * load driver profile
     *
     * @param driverId Driver Id
     * @return Array
     */
    public List<Long> loadProfile(long driverId) {
        List<Long> profileList = new ArrayList<>();
        ProfileDto profileDto = new ProfileDto();
        profileDto.setPage(new Pages().setSize(-1L)).setDriverId(driverId);
        R<Page<Profile>> rp = profileClient.list(profileDto);
        if (!rp.isOk()) {
            close();
            throw new ServiceException(rp.getMessage());
        }
        rp.getData().getRecords().forEach(profile -> profileList.add(profile.getId()));
        return profileList;
    }

    /**
     * 初始化设备
     *
     * @param profileList Profile Array
     */
    public void loadDevice(List<Long> profileList) {
        driverContext.setDeviceMap(new ConcurrentHashMap<>(16));
        driverContext.setDeviceNameMap(new ConcurrentHashMap<>(16));
        for (Long profileId : profileList) {
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
            R<Page<Device>> rp = deviceClient.list(deviceDto);
            if (!rp.isOk()) {
                close();
                throw new ServiceException(rp.getMessage());
            }
            for (Device device : rp.getData().getRecords()) {
                driverContext.getDeviceMap().put(device.getId(), device);
                driverContext.getDeviceNameMap().put(device.getName(), device.getId());
            }
        }
    }

    /**
     * 初始化位号
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     *
     * @param deviceMap Device Map
     */
    public void loadPoint(Map<Long, Device> deviceMap) {
        driverContext.setDevicePointInfoMap(new ConcurrentHashMap<>(16));
        driverContext.setDevicePointNameMap(new ConcurrentHashMap<>(16));
        for (Device device : deviceMap.values()) {
            Map<Long, Map<String, AttributeInfo>> infoMap = getDevicePointInfoMap(device);
            if (infoMap.size() > 0) {
                driverContext.getDevicePointInfoMap().put(device.getId(), infoMap);
            }
            Map<String, Long> nameMap = getDevicePointNameMap(device);
            if (nameMap.size() > 0) {
                driverContext.getDevicePointNameMap().put(device.getId(), nameMap);
            }
        }
    }

    /**
     * 获取模板驱动配置信息 Map
     *
     * @param profileId Profile Id
     * @return Map
     */
    public Map<String, AttributeInfo> getProfileDriverInfoMap(Long profileId) {
        Map<String, AttributeInfo> attributeInfoMap = new ConcurrentHashMap<>(16);
        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
        R<Page<DriverInfo>> rp = driverInfoClient.list(driverInfoDto);
        if (!rp.isOk()) {
            close();
            throw new ServiceException(rp.getMessage());
        }
        for (DriverInfo driverInfo : rp.getData().getRecords()) {
            DriverAttribute attribute = this.driverAttributeMap.get(driverInfo.getDriverAttributeId());
            attributeInfoMap.put(attribute.getName(), new AttributeInfo(driverInfo.getValue(), attribute.getType()));
        }
        return attributeInfoMap;
    }

    /**
     * 获取设备位号配置信息 Map
     *
     * @param device Device
     * @return Map
     */
    public Map<Long, Map<String, AttributeInfo>> getDevicePointInfoMap(Device device) {
        Map<Long, Map<String, AttributeInfo>> attributeInfoMap = new ConcurrentHashMap<>(16);
        Map<Long, Point> pointMap = driverContext.getProfilePointMap().get(device.getProfileId());
        for (Long pointId : pointMap.keySet()) {
            PointInfoDto pointInfoDto = new PointInfoDto();
            pointInfoDto.setPage(new Pages().setSize(-1L)).setDeviceId(device.getId()).setPointId(pointId);
            R<Page<PointInfo>> rp = pointInfoClient.list(pointInfoDto);
            if (!rp.isOk()) {
                close();
                throw new ServiceException(rp.getMessage());
            }
            Map<String, AttributeInfo> infoMap = new ConcurrentHashMap<>(16);
            List<PointInfo> pointInfos = rp.getData().getRecords();
            for (PointInfo pointInfo : pointInfos) {
                PointAttribute attribute = this.pointAttributeMap.get(pointInfo.getPointAttributeId());
                infoMap.put(attribute.getName(), new AttributeInfo(pointInfo.getValue(), attribute.getType()));
            }
            if (infoMap.size() > 0) {
                attributeInfoMap.put(pointId, infoMap);
            }
        }
        return attributeInfoMap;
    }

    /**
     * 获取设备位号名称 Map
     *
     * @param device Device
     * @return Map
     */
    public Map<String, Long> getDevicePointNameMap(Device device) {
        Map<String, Long> pointNameMap = new ConcurrentHashMap<>(16);
        Map<Long, Point> pointMap = driverContext.getProfilePointMap().get(device.getProfileId());
        for (Point point : pointMap.values()) {
            pointNameMap.put(point.getName(), point.getId());
        }
        return pointNameMap;
    }

    /**
     * 根据 Profile Id 获取位号 Map
     *
     * @param profileId Profile Id
     * @return Map
     */
    public Map<Long, Point> getPointMap(Long profileId) {
        Map<Long, Point> pointMap = new ConcurrentHashMap<>(16);
        PointDto pointDto = new PointDto();
        pointDto.setPage(new Pages().setSize(-1L)).setProfileId(profileId);
        R<Page<Point>> rp = pointClient.list(pointDto);
        if (!rp.isOk()) {
            close();
            throw new ServiceException(rp.getMessage());
        }
        for (Point point : rp.getData().getRecords()) {
            pointMap.put(point.getId(), point);
        }
        return pointMap;
    }

    /**
     * 关闭 ApplicationContext
     */
    private void close() {
        ((ConfigurableApplicationContext) applicationContext).close();
    }
}
