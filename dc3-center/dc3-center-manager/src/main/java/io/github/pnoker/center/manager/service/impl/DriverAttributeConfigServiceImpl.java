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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigPageQuery;
import io.github.pnoker.center.manager.mapper.DriverAttributeConfigMapper;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.DriverAttribute;
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
public class DriverAttributeConfigServiceImpl implements DriverAttributeConfigService {

    @Resource
    private DriverAttributeMapper driverAttributeMapper;
    @Resource
    private DriverAttributeConfigMapper driverAttributeConfigMapper;

    @Resource
    private NotifyService notifyService;

    @Override
    public void add(DriverAttributeConfig entityBO) {
        try {
            selectByDeviceIdAndAttributeId(entityBO.getDeviceId(), entityBO.getDriverAttributeId());
            throw new ServiceException("The driver attribute config already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeConfigMapper.insert(entityBO) < 1) {
                DriverAttribute driverAttribute = driverAttributeMapper.selectById(entityBO.getDriverAttributeId());
                throw new AddException("The driver attribute config {} add failed", driverAttribute.getDisplayName());
            }

            // 通知驱动新增
            DriverAttributeConfig driverAttributeConfig = driverAttributeConfigMapper.selectById(entityBO.getId());
            notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.ADD, driverAttributeConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        DriverAttributeConfig driverAttributeConfig = get(id);
        if (ObjectUtil.isNull(driverAttributeConfig)) {
            throw new NotFoundException("The driver attribute config does not exist");
        }

        if (driverAttributeConfigMapper.deleteById(id) < 1) {
            throw new DeleteException("The driver attribute config delete failed");
        }

        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.DELETE, driverAttributeConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverAttributeConfig entityBO) {
        DriverAttributeConfig oldDriverAttributeConfig = get(entityBO.getId());
        entityBO.setOperateTime(null);
        if (!oldDriverAttributeConfig.getDriverAttributeId().equals(entityBO.getDriverAttributeId()) || !oldDriverAttributeConfig.getDeviceId().equals(entityBO.getDeviceId())) {
            try {
                selectByDeviceIdAndAttributeId(entityBO.getDeviceId(), entityBO.getDriverAttributeId());
                throw new DuplicateException("The driver attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }

        if (driverAttributeConfigMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The driver attribute config update failed");
        }

        DriverAttributeConfig select = driverAttributeConfigMapper.selectById(entityBO.getId());
        entityBO.setDriverAttributeId(select.getDriverAttributeId());
        entityBO.setDeviceId(select.getDeviceId());
        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.UPDATE, select);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfig get(Long id) {
        DriverAttributeConfig driverAttributeConfig = driverAttributeConfigMapper.selectById(id);
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
        DriverAttributeConfig driverAttributeConfig = driverAttributeConfigMapper.selectOne(queryWrapper);
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
        List<DriverAttributeConfig> driverAttributeConfigs = driverAttributeConfigMapper.selectList(fuzzyQuery(driverInfoPageQuery));
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
        List<DriverAttributeConfig> driverAttributeConfigs = driverAttributeConfigMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (CollUtil.isEmpty(driverAttributeConfigs)) {
            return Collections.emptyList();
        }
        return driverAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttributeConfig> list(DriverAttributeConfigPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return driverAttributeConfigMapper.selectPage(entityQuery.getPage().page(), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<DriverAttributeConfig> fuzzyQuery(DriverAttributeConfigPageQuery query) {
        LambdaQueryWrapper<DriverAttributeConfig> queryWrapper = Wrappers.<DriverAttributeConfig>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDriverAttributeId()), DriverAttributeConfig::getDriverAttributeId, query.getDriverAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceId()), DriverAttributeConfig::getDeviceId, query.getDeviceId());
        }
        return queryWrapper;
    }

}
