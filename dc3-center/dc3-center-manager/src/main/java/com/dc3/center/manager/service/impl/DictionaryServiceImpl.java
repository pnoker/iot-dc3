/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dc3.center.manager.mapper.*;
import com.dc3.center.manager.service.DictionaryService;
import com.dc3.common.bean.Dictionary;
import com.dc3.common.constant.CacheConstant;
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
    private DeviceMapper deviceMapper;
    @Resource
    private PointMapper pointMapper;

    @Override
    @Cacheable(value = CacheConstant.Entity.DRIVER + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> driverDictionary(Long tenantId) {
        List<Dictionary> dictionaries = new ArrayList<>(16);
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.eq(Driver::getTenantId, tenantId);
        List<Driver> drivers = driverMapper.selectList(queryWrapper);
        drivers.forEach(driver -> dictionaries.add(new Dictionary().setLabel(driver.getName()).setValue(driver.getId())));
        return dictionaries;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.DRIVER_ATTRIBUTE + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> driverAttributeDictionary(Long tenantId) {
        List<Dictionary> dictionaries = driverDictionary(tenantId);
        dictionaries.forEach(driverDictionary -> {
            List<Dictionary> driverAttributeDictionaryList = new ArrayList<>(16);
            LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
            queryWrapper.eq(DriverAttribute::getDriverId, driverDictionary.getValue());
            List<DriverAttribute> driverAttributeList = driverAttributeMapper.selectList(queryWrapper);
            driverAttributeList.forEach(driverAttribute -> driverAttributeDictionaryList.add(new Dictionary().setLabel(driverAttribute.getDisplayName()).setValue(driverAttribute.getId())));

            driverDictionary.setDisabled(true);
            driverDictionary.setValue(RandomUtil.randomLong());
            driverDictionary.setChildren(driverAttributeDictionaryList);
        });
        return dictionaries;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_ATTRIBUTE + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> pointAttributeDictionary(Long tenantId) {
        List<Dictionary> dictionaries = driverDictionary(tenantId);
        dictionaries.forEach(driverDictionary -> {
            List<Dictionary> driverAttributeDictionaryList = new ArrayList<>(16);
            LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
            queryWrapper.eq(PointAttribute::getDriverId, driverDictionary.getValue());
            List<PointAttribute> pointAttributeList = pointAttributeMapper.selectList(queryWrapper);
            pointAttributeList.forEach(pointAttribute -> driverAttributeDictionaryList.add(new Dictionary().setLabel(pointAttribute.getDisplayName()).setValue(pointAttribute.getId())));

            driverDictionary.setDisabled(true);
            driverDictionary.setValue(RandomUtil.randomLong());
            driverDictionary.setChildren(driverAttributeDictionaryList);
        });
        return dictionaries;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.PROFILE + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> profileDictionary(Long tenantId) {
        List<Dictionary> dictionaries = new ArrayList<>(16);
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.eq(Profile::getTenantId, tenantId);
        List<Profile> profiles = profileMapper.selectList(queryWrapper);
        profiles.forEach(profile -> dictionaries.add(new Dictionary().setLabel(profile.getName()).setValue(profile.getId())));
        return dictionaries;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.DEVICE + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> deviceDictionary(Long tenantId) {
        List<Dictionary> dictionaries = driverDictionary(tenantId);
        dictionaries.forEach(driverDictionary -> {
            LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
            queryWrapper.eq(Device::getDriverId, driverDictionary.getValue());
            queryWrapper.eq(Device::getTenantId, tenantId);
            List<Device> deviceList = deviceMapper.selectList(queryWrapper);
            List<Dictionary> deviceDictionaryList = new ArrayList<>(16);
            deviceList.forEach(device -> deviceDictionaryList.add(new Dictionary().setLabel(device.getName()).setValue(device.getId())));
            driverDictionary.setChildren(deviceDictionaryList);
        });

        return dictionaries;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT + CacheConstant.Suffix.DIC, key = "'dic.'+#parent+'.'+#tenantId", unless = "#result==null")
    public List<Dictionary> pointDictionary(String parent, Long tenantId) {
        List<Dictionary> dictionaries = new ArrayList<>(16);
        switch (parent) {
            case "profile":
                List<Dictionary> profileDictionaryList = new ArrayList<>(16);

                LambdaQueryWrapper<Profile> profileQueryWrapper = Wrappers.<Profile>query().lambda();
                profileQueryWrapper.eq(Profile::getTenantId, tenantId);
                List<Profile> profileList = profileMapper.selectList(profileQueryWrapper);
                profileList.forEach(profile -> {
                    List<Dictionary> pointDictionaryList = new ArrayList<>(16);
                    LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
                    queryWrapper.eq(Point::getProfileId, profile.getId());
                    queryWrapper.eq(Point::getTenantId, tenantId);
                    List<Point> pointList = pointMapper.selectList(queryWrapper);
                    pointList.forEach(point -> pointDictionaryList.add(new Dictionary().setLabel(point.getName()).setValue(point.getId())));

                    Dictionary profileDictionary = new Dictionary().setLabel(profile.getName()).setValue(profile.getId());
                    profileDictionary.setChildren(pointDictionaryList);

                    profileDictionaryList.add(profileDictionary);
                });

                dictionaries = profileDictionaryList;
                break;
            case "device":
                List<Dictionary> deviceDictionaryList = new ArrayList<>(16);

                LambdaQueryWrapper<Device> deviceQueryWrapper = Wrappers.<Device>query().lambda();
                deviceQueryWrapper.eq(Device::getTenantId, tenantId);
                List<Device> deviceList = deviceMapper.selectList(deviceQueryWrapper);
                deviceList.forEach(device -> {
                    List<Dictionary> profileDictionaryLists = new ArrayList<>(16);
                    device.getProfileIds().forEach(profileId -> {
                        Profile profile = profileMapper.selectById(profileId);

                        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
                        queryWrapper.eq(Point::getProfileId, profileId);
                        queryWrapper.eq(Point::getTenantId, tenantId);
                        List<Point> pointList = pointMapper.selectList(queryWrapper);
                        List<Dictionary> pointDictionaryList = new ArrayList<>(16);
                        pointList.forEach(point -> pointDictionaryList.add(new Dictionary().setLabel(point.getName()).setValue(point.getId())));

                        Dictionary profileDictionary = new Dictionary().setLabel(profile.getName()).setValue(profileId);
                        profileDictionary.setChildren(pointDictionaryList);

                        profileDictionaryLists.add(profileDictionary);
                    });

                    Dictionary deviceDictionary = new Dictionary().setLabel(device.getName()).setValue(device.getId());
                    deviceDictionary.setChildren(profileDictionaryLists);

                    deviceDictionaryList.add(deviceDictionary);
                });

                dictionaries = deviceDictionaryList;
                break;
            case "point":
                List<Dictionary> pointDictionaryList = new ArrayList<>(16);

                LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
                queryWrapper.eq(Point::getTenantId, tenantId);
                List<Point> pointList = pointMapper.selectList(queryWrapper);
                pointList.forEach(point -> pointDictionaryList.add(new Dictionary().setLabel(point.getName()).setValue(point.getId())));

                dictionaries = pointDictionaryList;
                break;
            default:
                break;
        }
        return dictionaries;
    }
}
