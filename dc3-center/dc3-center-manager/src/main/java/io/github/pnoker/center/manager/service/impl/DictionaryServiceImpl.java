/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.*;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.bean.common.Dictionary;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.api.center.manager.dto.DictionaryDto;
import io.github.pnoker.api.center.manager.dto.PointDto;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Driver;
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
    private PointService pointService;

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

    @Override
    public Page<Dictionary> driverDictionary(DictionaryDto dictionaryDto) {
        if (ObjectUtil.isNull(dictionaryDto.getPage())) {
            dictionaryDto.setPage(new Pages());
        }
        LambdaQueryWrapper<Driver> queryWrapper = Wrappers.<Driver>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryDto.getLabel()), Driver::getName, dictionaryDto.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryDto.getTenantId()), Driver::getTenantId, dictionaryDto.getTenantId());
        Page<Driver> driverPage = driverMapper.selectPage(dictionaryDto.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = driverPage.getRecords().parallelStream().map(driver -> new Dictionary().setLabel(driver.getName()).setValue(driver.getId())).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(driverPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> deviceDictionary(DictionaryDto dictionaryDto) {
        if (ObjectUtil.isNull(dictionaryDto.getPage())) {
            dictionaryDto.setPage(new Pages());
        }
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotBlank(dictionaryDto.getLabel()), Device::getName, dictionaryDto.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryDto.getParentValue1()), Device::getDriverId, dictionaryDto.getParentValue1());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryDto.getTenantId()), Device::getTenantId, dictionaryDto.getTenantId());
        Page<Device> devicePage = deviceMapper.selectPage(dictionaryDto.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = devicePage.getRecords().parallelStream().map(profile -> new Dictionary().setLabel(profile.getName()).setValue(profile.getId())).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(devicePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> profileDictionary(DictionaryDto dictionaryDto) {
        if (ObjectUtil.isNull(dictionaryDto.getPage())) {
            dictionaryDto.setPage(new Pages());
        }
        LambdaQueryWrapper<Profile> queryWrapper = Wrappers.<Profile>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryDto.getLabel()), Profile::getName, dictionaryDto.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryDto.getTenantId()), Profile::getTenantId, dictionaryDto.getTenantId());
        Page<Profile> profilePage = profileMapper.selectPage(dictionaryDto.getPage().convert(), queryWrapper);
        List<Dictionary> dictionaryList = profilePage.getRecords().parallelStream().map(profile -> new Dictionary().setLabel(profile.getName()).setValue(profile.getId())).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(profilePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> pointDictionary(DictionaryDto dictionaryDto) {
        if (ObjectUtil.isNull(dictionaryDto.getPage())) {
            dictionaryDto.setPage(new Pages());
        }
        PointDto pointDto = (new PointDto()).setPage(dictionaryDto.getPage());
        pointDto.setDeviceId(dictionaryDto.getParentValue2());
        pointDto.setName(dictionaryDto.getLabel()).setProfileId(dictionaryDto.getParentValue1()).setTenantId(dictionaryDto.getTenantId());
        Page<Point> pointPage = pointService.list(pointDto);
        List<Dictionary> dictionaryList = pointPage.getRecords().parallelStream().map(profile -> new Dictionary().setLabel(profile.getName()).setValue(profile.getId())).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(pointPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

}
