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
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigBOPageQuery;
import io.github.pnoker.center.manager.mapper.DriverAttributeConfigMapper;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
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
    public void save(DriverAttributeConfigBO entityBO) {
        try {
            selectByDeviceIdAndAttributeId(entityBO.getDeviceId(), entityBO.getDriverAttributeId());
            throw new ServiceException("The driver attribute config already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeConfigMapper.insert(entityBO) < 1) {
                DriverAttributeBO driverAttributeBO = driverAttributeMapper.selectById(entityBO.getDriverAttributeId());
                throw new AddException("The driver attribute config {} add failed", driverAttributeBO.getDisplayName());
            }

            // 通知驱动新增
            DriverAttributeConfigBO driverAttributeConfigBO = driverAttributeConfigMapper.selectById(entityBO.getId());
            notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.ADD, driverAttributeConfigBO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        DriverAttributeConfigBO driverAttributeConfigBO = selectById(id);
        if (ObjectUtil.isNull(driverAttributeConfigBO)) {
            throw new NotFoundException("The driver attribute config does not exist");
        }

        if (driverAttributeConfigMapper.deleteById(id) < 1) {
            throw new DeleteException("The driver attribute config delete failed");
        }

        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.DELETE, driverAttributeConfigBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverAttributeConfigBO entityBO) {
        DriverAttributeConfigBO oldDriverAttributeConfigBO = selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (!oldDriverAttributeConfigBO.getDriverAttributeId().equals(entityBO.getDriverAttributeId()) || !oldDriverAttributeConfigBO.getDeviceId().equals(entityBO.getDeviceId())) {
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

        DriverAttributeConfigBO select = driverAttributeConfigMapper.selectById(entityBO.getId());
        entityBO.setDriverAttributeId(select.getDriverAttributeId());
        entityBO.setDeviceId(select.getDeviceId());
        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.UPDATE, select);
    }

    @Override
    public DriverAttributeConfigBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfigBO selectByDeviceIdAndAttributeId(Long deviceId, Long driverAttributeId) {
        DriverAttributeConfigBOPageQuery driverInfoPageQuery = new DriverAttributeConfigBOPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        driverInfoPageQuery.setDeviceId(deviceId);
        LambdaQueryWrapper<DriverAttributeConfigBO> queryWrapper = fuzzyQuery(driverInfoPageQuery);
        queryWrapper.last("limit 1");
        DriverAttributeConfigBO driverAttributeConfigBO = driverAttributeConfigMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driverAttributeConfigBO)) {
            throw new NotFoundException();
        }
        return driverAttributeConfigBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfigBO> selectByAttributeId(Long driverAttributeId) {
        DriverAttributeConfigBOPageQuery driverInfoPageQuery = new DriverAttributeConfigBOPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        List<DriverAttributeConfigBO> driverAttributeConfigBOS = driverAttributeConfigMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (ObjectUtil.isNull(driverAttributeConfigBOS) || driverAttributeConfigBOS.isEmpty()) {
            throw new NotFoundException();
        }
        return driverAttributeConfigBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfigBO> selectByDeviceId(Long deviceId) {
        DriverAttributeConfigBOPageQuery driverInfoPageQuery = new DriverAttributeConfigBOPageQuery();
        driverInfoPageQuery.setDeviceId(deviceId);
        List<DriverAttributeConfigBO> driverAttributeConfigBOS = driverAttributeConfigMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (CollUtil.isEmpty(driverAttributeConfigBOS)) {
            return Collections.emptyList();
        }
        return driverAttributeConfigBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttributeConfigBO> selectByPage(DriverAttributeConfigBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return driverAttributeConfigMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<DriverAttributeConfigBO> fuzzyQuery(DriverAttributeConfigBOPageQuery query) {
        LambdaQueryWrapper<DriverAttributeConfigBO> queryWrapper = Wrappers.<DriverAttributeConfigBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDriverAttributeId()), DriverAttributeConfigBO::getDriverAttributeId, query.getDriverAttributeId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDeviceId()), DriverAttributeConfigBO::getDeviceId, query.getDeviceId());
        }
        return queryWrapper;
    }

}
