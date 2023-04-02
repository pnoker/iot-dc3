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
import io.github.pnoker.center.manager.mapper.DriverAttributeConfigMapper;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
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
public class DriverAttributeConfigServiceImpl implements DriverAttributeConfigService {

    @Resource
    private DriverAttributeConfigMapper driverAttributeConfigMapper;

    @Resource
    private NotifyService notifyService;

    @Override
    public void add(DriverAttributeConfig entityDO) {
        try {
            selectByDeviceIdAndAttributeId(entityDO.getDeviceId(), entityDO.getDriverAttributeId());
            throw new ServiceException("The driver attribute config already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeConfigMapper.insert(entityDO) > 0) {
                DriverAttributeConfig add = driverAttributeConfigMapper.selectById(entityDO.getId());
                notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.ADD, add);
            }
            throw new ServiceException("The driver attribute config add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        DriverAttributeConfig driverAttributeConfig = selectById(id);
        boolean delete = driverAttributeConfigMapper.deleteById(id) > 0;
        if (delete) {
            notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.DELETE, driverAttributeConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverAttributeConfig entityDO) {
        DriverAttributeConfig oldDriverAttributeConfig = selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (!oldDriverAttributeConfig.getDriverAttributeId().equals(entityDO.getDriverAttributeId()) || !oldDriverAttributeConfig.getDeviceId().equals(entityDO.getDeviceId())) {
            try {
                selectByDeviceIdAndAttributeId(entityDO.getDeviceId(), entityDO.getDriverAttributeId());
                throw new DuplicateException("The driver attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (driverAttributeConfigMapper.updateById(entityDO) > 0) {
            DriverAttributeConfig select = driverAttributeConfigMapper.selectById(entityDO.getId());
            entityDO.setDriverAttributeId(select.getDriverAttributeId());
            entityDO.setDeviceId(select.getDeviceId());
            notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.UPDATE, select);
        }
        throw new ServiceException("The driver attribute config update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfig selectById(String id) {
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
    public Page<DriverAttributeConfig> list(DriverAttributeConfigPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return driverAttributeConfigMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    public LambdaQueryWrapper<DriverAttributeConfig> fuzzyQuery(DriverAttributeConfigPageQuery query) {
        LambdaQueryWrapper<DriverAttributeConfig> queryWrapper = Wrappers.<DriverAttributeConfig>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDriverAttributeId()), DriverAttributeConfig::getDriverAttributeId, query.getDriverAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceId()), DriverAttributeConfig::getDeviceId, query.getDeviceId());
        }
        return queryWrapper;
    }

}
