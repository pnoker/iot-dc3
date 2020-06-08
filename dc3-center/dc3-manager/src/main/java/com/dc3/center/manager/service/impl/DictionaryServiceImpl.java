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

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dc3.center.manager.mapper.*;
import com.dc3.center.manager.service.DictionaryService;
import com.dc3.common.bean.Dictionary;
import com.dc3.common.constant.Common;
import com.dc3.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {
    @Resource
    private DriverMapper driverMapper;
    @Resource
    private DriverAttributeMapper driverAttributeMapper;
    @Resource
    private PointAttributeMapper pointAttributeMapper;
    @Resource
    private ProfileMapper profileMapper;
    @Resource
    private GroupMapper groupMapper;
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private PointMapper pointMapper;

    @Override
    @Cacheable(value = Common.Cache.DRIVER + Common.Cache.DIC, key = "'dirver_dic'", unless = "#result==null")
    public List<Dictionary> driverDictionary() {
        List<Dictionary> dictionaryList = new ArrayList<>();
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        List<Driver> driverList = driverMapper.selectList(queryWrapper);
        for (Driver driver : driverList) {
            Dictionary driverDictionary = new Dictionary().setLabel(driver.getName()).setValue(driver.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_ATTRIBUTE + Common.Cache.DIC, key = "'driver_attribute_dic'", unless = "#result==null")
    public List<Dictionary> driverAttributeDictionary() {
        List<Dictionary> driverDictionaryList = driverDictionary();
        for (Dictionary driverDictionary : driverDictionaryList) {
            List<Dictionary> driverAttributeDictionaryList = new ArrayList<>();
            LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
            queryWrapper.eq(DriverAttribute::getDriverId, driverDictionary.getValue());
            List<DriverAttribute> driverAttributeList = driverAttributeMapper.selectList(queryWrapper);
            driverDictionary.setDisabled(true);
            driverDictionary.setValue(RandomUtil.randomLong());
            for (DriverAttribute driverAttribute : driverAttributeList) {
                Dictionary driverAttributeDictionary = new Dictionary().setLabel(driverAttribute.getDisplayName()).setValue(driverAttribute.getId());
                driverAttributeDictionaryList.add(driverAttributeDictionary);
            }
            driverDictionary.setChildren(driverAttributeDictionaryList);
        }
        return driverDictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, key = "'point_attribute_dic'", unless = "#result==null")
    public List<Dictionary> pointAttributeDictionary() {
        List<Dictionary> driverDictionaryList = driverDictionary();
        for (Dictionary driverDictionary : driverDictionaryList) {
            List<Dictionary> driverAttributeDictionaryList = new ArrayList<>();
            LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
            queryWrapper.eq(PointAttribute::getDriverId, driverDictionary.getValue());
            List<PointAttribute> pointAttributeList = pointAttributeMapper.selectList(queryWrapper);
            driverDictionary.setDisabled(true);
            driverDictionary.setValue(RandomUtil.randomLong());
            for (PointAttribute pointAttribute : pointAttributeList) {
                Dictionary pointAttributeDictionary = new Dictionary().setLabel(pointAttribute.getDisplayName()).setValue(pointAttribute.getId());
                driverAttributeDictionaryList.add(pointAttributeDictionary);
            }
            driverDictionary.setChildren(driverAttributeDictionaryList);
        }
        return driverDictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE + Common.Cache.DIC, key = "'profile_dic'", unless = "#result==null")
    public List<Dictionary> profileDictionary() {
        List<Dictionary> driverDictionaryList = driverDictionary();
        for (Dictionary driverDictionary : driverDictionaryList) {
            List<Dictionary> profileDictionaryList = new ArrayList<>();
            LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
            queryWrapper.eq(Profile::getDriverId, driverDictionary.getValue());
            List<Profile> profileList = profileMapper.selectList(queryWrapper);
            driverDictionary.setDisabled(true);
            driverDictionary.setValue(RandomUtil.randomLong());
            for (Profile profile : profileList) {
                Dictionary profileDictionary = new Dictionary().setLabel(profile.getName()).setValue(profile.getId());
                profileDictionaryList.add(profileDictionary);
            }
            driverDictionary.setChildren(profileDictionaryList);
        }
        return driverDictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.GROUP + Common.Cache.DIC, key = "'group_dic'", unless = "#result==null")
    public List<Dictionary> groupDictionary() {
        List<Dictionary> dictionaryList = new ArrayList<>();
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        List<Group> groupList = groupMapper.selectList(queryWrapper);
        for (Group group : groupList) {
            Dictionary groupDictionary = new Dictionary().setLabel(group.getName()).setValue(group.getId());
            dictionaryList.add(groupDictionary);
        }
        return dictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.DIC, key = "'device_dic.'+#parent", unless = "#result==null")
    public List<Dictionary> deviceDictionary(String parent) {
        List<Dictionary> dictionaryList = new ArrayList<>();
        switch (parent) {
            case "group":
                List<Dictionary> groupDictionaryList = groupDictionary();
                for (Dictionary groupDictionary : groupDictionaryList) {
                    List<Dictionary> deviceDictionaryList = new ArrayList<>();
                    LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
                    queryWrapper.eq(Device::getGroupId, groupDictionary.getValue());
                    List<Device> deviceList = deviceMapper.selectList(queryWrapper);
                    groupDictionary.setDisabled(true);
                    groupDictionary.setValue(RandomUtil.randomLong());
                    for (Device device : deviceList) {
                        Dictionary deviceDictionary = new Dictionary().setLabel(device.getName()).setValue(device.getId());
                        deviceDictionaryList.add(deviceDictionary);
                    }
                    groupDictionary.setChildren(deviceDictionaryList);
                }
                dictionaryList = groupDictionaryList;
                break;
            case "driver":
                List<Dictionary> driverDictionaryList = profileDictionary();
                for (Dictionary driverDictionary : driverDictionaryList) {
                    for (Dictionary profileDictionary : driverDictionary.getChildren()) {
                        List<Dictionary> deviceDictionaryList = new ArrayList<>();
                        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
                        queryWrapper.eq(Device::getProfileId, profileDictionary.getValue());
                        List<Device> deviceList = deviceMapper.selectList(queryWrapper);
                        profileDictionary.setDisabled(true);
                        profileDictionary.setValue(RandomUtil.randomLong());
                        for (Device device : deviceList) {
                            Dictionary deviceDictionary = new Dictionary().setLabel(device.getName()).setValue(device.getId());
                            deviceDictionaryList.add(deviceDictionary);
                        }
                        profileDictionary.setChildren(deviceDictionaryList);
                    }
                }
                dictionaryList = driverDictionaryList;
                break;
            case "profile":
                List<Dictionary> profileDictionaryList = new ArrayList<>();
                List<Profile> profileList = profileMapper.selectList(Wrappers.<Profile>query().lambda());
                for (Profile profile : profileList) {
                    Dictionary profileDictionary = new Dictionary().setLabel(profile.getName()).setValue(profile.getId());
                    List<Dictionary> deviceDictionaryList = new ArrayList<>();
                    LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
                    queryWrapper.eq(Device::getProfileId, profileDictionary.getValue());
                    List<Device> deviceList = deviceMapper.selectList(queryWrapper);
                    profileDictionary.setDisabled(true);
                    profileDictionary.setValue(RandomUtil.randomLong());
                    for (Device device : deviceList) {
                        Dictionary deviceDictionary = new Dictionary().setLabel(device.getName()).setValue(device.getId());
                        deviceDictionaryList.add(deviceDictionary);
                    }
                    profileDictionary.setChildren(deviceDictionaryList);
                    profileDictionaryList.add(profileDictionary);
                }
                dictionaryList = profileDictionaryList;
                break;
            default:
                break;
        }
        return dictionaryList;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.DIC, key = "'point_dic.'+#parent", unless = "#result==null")
    public List<Dictionary> pointDictionary(String parent) {
        List<Dictionary> dictionaryList = new ArrayList<>();
        switch (parent) {
            case "profile":
                List<Dictionary> profileDictionaryList = new ArrayList<>();
                List<Profile> profileList = profileMapper.selectList(Wrappers.<Profile>query().lambda());
                for (Profile profile : profileList) {
                    Dictionary profileDictionary = new Dictionary().setLabel(profile.getName()).setValue(profile.getId());
                    List<Dictionary> pointDictionaryList = new ArrayList<>();
                    LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
                    queryWrapper.eq(Point::getProfileId, profile.getId());
                    List<Point> pointList = pointMapper.selectList(queryWrapper);
                    profileDictionary.setDisabled(true);
                    profileDictionary.setValue(RandomUtil.randomLong());
                    for (Point point : pointList) {
                        Dictionary pointDictionary = new Dictionary().setLabel(point.getName()).setValue(point.getId());
                        pointDictionaryList.add(pointDictionary);
                    }
                    profileDictionary.setChildren(pointDictionaryList);
                    profileDictionaryList.add(profileDictionary);
                }
                dictionaryList = profileDictionaryList;
                break;
            case "device":
                List<Dictionary> deviceDictionaryList = new ArrayList<>();
                List<Device> deviceList = deviceMapper.selectList(Wrappers.<Device>query().lambda());
                for (Device device : deviceList) {
                    Dictionary deviceDictionary = new Dictionary().setLabel(device.getName()).setValue(device.getId());
                    List<Dictionary> pointDictionaryList = new ArrayList<>();
                    LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
                    queryWrapper.eq(Point::getProfileId, device.getProfileId());
                    List<Point> pointList = pointMapper.selectList(queryWrapper);
                    deviceDictionary.setDisabled(true);
                    deviceDictionary.setValue(RandomUtil.randomLong());
                    for (Point point : pointList) {
                        Dictionary pointDictionary = new Dictionary().setLabel(point.getName()).setValue(point.getId());
                        pointDictionaryList.add(pointDictionary);
                    }
                    deviceDictionary.setChildren(pointDictionaryList);
                    deviceDictionaryList.add(deviceDictionary);
                }
                dictionaryList = deviceDictionaryList;
                break;
            default:
                break;
        }
        return dictionaryList;
    }
}
