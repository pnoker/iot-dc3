/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DictionaryPageQuery;
import io.github.pnoker.center.manager.entity.query.PointPageQuery;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Dictionary;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.DriverDO;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Resource
    private DriverMapper driverMapper;
    @Resource
    private ProfileMapper profileMapper;
    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private PointService pointService;

    @Override
    public Page<Dictionary> driverDictionary(DictionaryPageQuery dictionaryPageQuery) {
        if (ObjectUtil.isNull(dictionaryPageQuery.getPage())) {
            dictionaryPageQuery.setPage(new Pages());
        }
        LambdaQueryWrapper<DriverDO> queryWrapper = Wrappers.<DriverDO>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getLabel()), DriverDO::getDriverName, dictionaryPageQuery.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getTenantId()), DriverDO::getTenantId, dictionaryPageQuery.getTenantId());
        Page<DriverDO> driverPage = driverMapper.selectPage(dictionaryPageQuery.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = driverPage.getRecords().parallelStream().map(driver -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(driver.getDriverName());
            dictionary.setValue(driver.getId());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(driverPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> deviceDictionary(DictionaryPageQuery dictionaryPageQuery) {
        if (ObjectUtil.isNull(dictionaryPageQuery.getPage())) {
            dictionaryPageQuery.setPage(new Pages());
        }
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getLabel()), Device::getDeviceName, dictionaryPageQuery.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getParentValue1()), Device::getDriverId, dictionaryPageQuery.getParentValue1());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getTenantId()), Device::getTenantId, dictionaryPageQuery.getTenantId());
        Page<Device> devicePage = deviceMapper.selectPage(dictionaryPageQuery.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = devicePage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getDeviceName());
            dictionary.setValue(profile.getId());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(devicePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> profileDictionary(DictionaryPageQuery dictionaryPageQuery) {
        if (ObjectUtil.isNull(dictionaryPageQuery.getPage())) {
            dictionaryPageQuery.setPage(new Pages());
        }
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getLabel()), Profile::getProfileName, dictionaryPageQuery.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryPageQuery.getTenantId()), Profile::getTenantId, dictionaryPageQuery.getTenantId());
        Page<Profile> profilePage = profileMapper.selectPage(dictionaryPageQuery.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = profilePage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getProfileName());
            dictionary.setValue(profile.getId());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(profilePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> pointDictionary(DictionaryPageQuery dictionaryPageQuery) {
        if (ObjectUtil.isNull(dictionaryPageQuery.getPage())) {
            dictionaryPageQuery.setPage(new Pages());
        }
        PointPageQuery pointPageQuery = new PointPageQuery();
        pointPageQuery.setPage(dictionaryPageQuery.getPage());
        pointPageQuery.setDeviceId(dictionaryPageQuery.getParentValue2());
        pointPageQuery.setPointName(dictionaryPageQuery.getLabel());
        pointPageQuery.setProfileId(dictionaryPageQuery.getParentValue1());
        pointPageQuery.setTenantId(dictionaryPageQuery.getTenantId());
        Page<Point> pointPage = pointService.list(pointPageQuery);
        List<Dictionary> dictionaryList = pointPage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getPointName());
            dictionary.setValue(profile.getId());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(pointPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

}
