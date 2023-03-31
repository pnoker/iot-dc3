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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigPageQuery;
import io.github.pnoker.center.manager.mapper.DriverInfoMapper;
import io.github.pnoker.center.manager.service.DriverInfoService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.DriverAttributeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * DriverInfoService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverInfoServiceImpl implements DriverInfoService {

    @Resource
    private DriverInfoMapper driverInfoMapper;

    @Override
    public DriverAttributeConfig add(DriverAttributeConfig driverAttributeConfig) {
        try {
            selectByDeviceIdAndAttributeId(driverAttributeConfig.getDeviceId(), driverAttributeConfig.getDriverAttributeId());
            throw new ServiceException("The driver attribute config already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverInfoMapper.insert(driverAttributeConfig) > 0) {
                return driverInfoMapper.selectById(driverAttributeConfig.getId());
            }
            throw new ServiceException("The driver attribute config add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return driverInfoMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfig update(DriverAttributeConfig driverAttributeConfig) {
        DriverAttributeConfig oldDriverAttributeConfig = selectById(driverAttributeConfig.getId());
        driverAttributeConfig.setOperateTime(null);
        if (!oldDriverAttributeConfig.getDriverAttributeId().equals(driverAttributeConfig.getDriverAttributeId()) || !oldDriverAttributeConfig.getDeviceId().equals(driverAttributeConfig.getDeviceId())) {
            try {
                selectByDeviceIdAndAttributeId(driverAttributeConfig.getDeviceId(), driverAttributeConfig.getDriverAttributeId());
                throw new DuplicateException("The driver attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (driverInfoMapper.updateById(driverAttributeConfig) > 0) {
            DriverAttributeConfig select = driverInfoMapper.selectById(driverAttributeConfig.getId());
            driverAttributeConfig.setDriverAttributeId(select.getDriverAttributeId());
            driverAttributeConfig.setDeviceId(select.getDeviceId());
            return select;
        }
        throw new ServiceException("The driver attribute config update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfig selectById(String id) {
        DriverAttributeConfig driverAttributeConfig = driverInfoMapper.selectById(id);
        if (ObjectUtil.isNull(driverAttributeConfig)) {
            throw new NotFoundException();
        }
        return driverAttributeConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfig selectByDeviceIdAndAttributeId(String deviceId, String driverAttributeId) {
        DriverAttributeConfigPageQuery driverInfoPageQuery = new DriverAttributeConfigPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        driverInfoPageQuery.setDeviceId(deviceId);
        LambdaQueryWrapper<DriverAttributeConfig> queryWrapper = fuzzyQuery(driverInfoPageQuery);
        queryWrapper.last("limit 1");
        DriverAttributeConfig driverAttributeConfig = driverInfoMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driverAttributeConfig)) {
            throw new NotFoundException();
        }
        return driverAttributeConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfig> selectByAttributeId(String driverAttributeId) {
        DriverAttributeConfigPageQuery driverInfoPageQuery = new DriverAttributeConfigPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        List<DriverAttributeConfig> driverAttributeConfigs = driverInfoMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (ObjectUtil.isNull(driverAttributeConfigs) || driverAttributeConfigs.isEmpty()) {
            throw new NotFoundException();
        }
        return driverAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfig> selectByDeviceId(String deviceId) {
        DriverAttributeConfigPageQuery driverInfoPageQuery = new DriverAttributeConfigPageQuery();
        driverInfoPageQuery.setDeviceId(deviceId);
        List<DriverAttributeConfig> driverAttributeConfigs = driverInfoMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (CollUtil.isEmpty(driverAttributeConfigs)) {
            return Collections.emptyList();
        }
        return driverAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttributeConfig> list(DriverAttributeConfigPageQuery driverInfoPageQuery) {
        if (ObjectUtil.isNull(driverInfoPageQuery.getPage())) {
            driverInfoPageQuery.setPage(new Pages());
        }
        return driverInfoMapper.selectPage(driverInfoPageQuery.getPage().convert(), fuzzyQuery(driverInfoPageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<DriverAttributeConfig> fuzzyQuery(DriverAttributeConfigPageQuery driverInfoPageQuery) {
        LambdaQueryWrapper<DriverAttributeConfig> queryWrapper = Wrappers.<DriverAttributeConfig>query().lambda();
        if (ObjectUtil.isNotNull(driverInfoPageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverInfoPageQuery.getDriverAttributeId()), DriverAttributeConfig::getDriverAttributeId, driverInfoPageQuery.getDriverAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverInfoPageQuery.getDeviceId()), DriverAttributeConfig::getDeviceId, driverInfoPageQuery.getDeviceId());
        }
        return queryWrapper;
    }

}
