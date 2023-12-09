/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.model.ProfileDO;
import io.github.pnoker.center.manager.entity.query.DictionaryQuery;
import io.github.pnoker.center.manager.entity.query.PointBOPageQuery;
import io.github.pnoker.center.manager.manager.DriverManager;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.mapper.DriverMapper;
import io.github.pnoker.center.manager.mapper.ProfileMapper;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Dictionary;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.utils.PageUtil;
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
    private DriverManager driverManager;
    @Resource
    private ProfileMapper profileMapper;
    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private PointService pointService;

    @Override
    public Page<Dictionary> driverDictionary(DictionaryQuery dictionaryQuery) {
        if (ObjectUtil.isNull(dictionaryQuery.getPages())) {
            dictionaryQuery.setPages(new Pages());
        }
        LambdaQueryWrapper<DriverBO> queryWrapper = Wrappers.<DriverBO>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryQuery.getLabel()), DriverBO::getDriverName, dictionaryQuery.getLabel());
        queryWrapper.eq(ObjectUtil.isNotEmpty(dictionaryQuery.getTenantId()), DriverBO::getTenantId, dictionaryQuery.getTenantId());
        Page<DriverBO> driverPage = driverManager.page(PageUtil.page(dictionaryQuery.getPages()), queryWrapper);
        List<Dictionary> dictionaryList = driverPage.getRecords().parallelStream().map(driver -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(driver.getDriverName());
            dictionary.setValue(driver.getId().toString());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(driverPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> deviceDictionary(DictionaryQuery dictionaryQuery) {
        if (ObjectUtil.isNull(dictionaryQuery.getPages())) {
            dictionaryQuery.setPages(new Pages());
        }
        LambdaQueryWrapper<DeviceDO> queryWrapper = Wrappers.<DeviceDO>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryQuery.getLabel()), DeviceDO::getDeviceName, dictionaryQuery.getLabel());
        queryWrapper.eq(CharSequenceUtil.isNotEmpty(dictionaryQuery.getValue1()), DeviceDO::getDriverId, dictionaryQuery.getValue1());
        queryWrapper.eq(ObjectUtil.isNotEmpty(dictionaryQuery.getTenantId()), DeviceDO::getTenantId, dictionaryQuery.getTenantId());
        Page<DeviceDO> devicePage = driverManager.page(PageUtil.page(dictionaryQuery.getPages()), queryWrapper);
        List<Dictionary> dictionaryList = devicePage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getDeviceName());
            dictionary.setValue(profile.getId().toString());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(devicePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> profileDictionary(DictionaryQuery dictionaryQuery) {
        if (ObjectUtil.isNull(dictionaryQuery.getPages())) {
            dictionaryQuery.setPages(new Pages());
        }
        LambdaQueryWrapper<ProfileDO> queryWrapper = Wrappers.<ProfileDO>query().lambda();
        queryWrapper.like(CharSequenceUtil.isNotEmpty(dictionaryQuery.getLabel()), ProfileDO::getProfileName, dictionaryQuery.getLabel());
        queryWrapper.eq(ObjectUtil.isNotEmpty(dictionaryQuery.getTenantId()), ProfileDO::getTenantId, dictionaryQuery.getTenantId());
        Page<ProfileDO> profilePage = profileMapper.selectPage(PageUtil.page(dictionaryQuery.getPages()), queryWrapper);
        List<Dictionary> dictionaryList = profilePage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getProfileName());
            dictionary.setValue(profile.getId().toString());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(profilePage, page);
        page.setRecords(dictionaryList);
        return page;
    }

    @Override
    public Page<Dictionary> pointDictionary(DictionaryQuery dictionaryQuery) {
        if (ObjectUtil.isNull(dictionaryQuery.getPages())) {
            dictionaryQuery.setPages(new Pages());
        }
        PointBOPageQuery pointPageQuery = new PointBOPageQuery();
        pointPageQuery.setPage(dictionaryQuery.getPages());
        pointPageQuery.setDeviceId(Long.parseLong(dictionaryQuery.getValue2()));
        pointPageQuery.setPointName(dictionaryQuery.getLabel());
        pointPageQuery.setProfileId(Long.parseLong(dictionaryQuery.getValue1()));
        pointPageQuery.setTenantId(dictionaryQuery.getTenantId());
        Page<PointBO> pointPage = pointService.selectByPage(pointPageQuery);
        List<Dictionary> dictionaryList = pointPage.getRecords().parallelStream().map(profile -> {
            Dictionary dictionary = new Dictionary();
            dictionary.setLabel(profile.getPointName());
            dictionary.setValue(profile.getId().toString());
            return dictionary;
        }).collect(Collectors.toList());
        Page<Dictionary> page = new Page<>();
        BeanUtils.copyProperties(pointPage, page);
        page.setRecords(dictionaryList);
        return page;
    }

}
