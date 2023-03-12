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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DriverInfoPageQuery;
import io.github.pnoker.center.manager.mapper.DriverInfoMapper;
import io.github.pnoker.center.manager.service.DriverInfoService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.DriverInfo;
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
    public DriverInfo add(DriverInfo driverInfo) {
        try {
            selectByDeviceIdAndAttributeId(driverInfo.getDeviceId(), driverInfo.getDriverAttributeId());
            throw new ServiceException("The driver info already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverInfoMapper.insert(driverInfo) > 0) {
                return driverInfoMapper.selectById(driverInfo.getId());
            }
            throw new ServiceException("The driver info add failed");
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
    public DriverInfo update(DriverInfo driverInfo) {
        DriverInfo oldDriverInfo = selectById(driverInfo.getId());
        driverInfo.setUpdateTime(null);
        if (!oldDriverInfo.getDriverAttributeId().equals(driverInfo.getDriverAttributeId()) || !oldDriverInfo.getDeviceId().equals(driverInfo.getDeviceId())) {
            try {
                selectByDeviceIdAndAttributeId(driverInfo.getDeviceId(), driverInfo.getDriverAttributeId());
                throw new DuplicateException("The driver info already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (driverInfoMapper.updateById(driverInfo) > 0) {
            DriverInfo select = driverInfoMapper.selectById(driverInfo.getId());
            driverInfo.setDriverAttributeId(select.getDriverAttributeId());
            driverInfo.setDeviceId(select.getDeviceId());
            return select;
        }
        throw new ServiceException("The driver info update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverInfo selectById(String id) {
        DriverInfo driverInfo = driverInfoMapper.selectById(id);
        if (null == driverInfo) {
            throw new NotFoundException();
        }
        return driverInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverInfo selectByDeviceIdAndAttributeId(String deviceId, String driverAttributeId) {
        DriverInfoPageQuery driverInfoPageQuery = new DriverInfoPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        driverInfoPageQuery.setDeviceId(deviceId);
        LambdaQueryWrapper<DriverInfo> queryWrapper = fuzzyQuery(driverInfoPageQuery);
        queryWrapper.last("limit 1");
        DriverInfo driverInfo = driverInfoMapper.selectOne(queryWrapper);
        if (null == driverInfo) {
            throw new NotFoundException();
        }
        return driverInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverInfo> selectByAttributeId(String driverAttributeId) {
        DriverInfoPageQuery driverInfoPageQuery = new DriverInfoPageQuery();
        driverInfoPageQuery.setDriverAttributeId(driverAttributeId);
        List<DriverInfo> driverInfos = driverInfoMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (null == driverInfos || driverInfos.isEmpty()) {
            throw new NotFoundException();
        }
        return driverInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverInfo> selectByDeviceId(String deviceId) {
        DriverInfoPageQuery driverInfoPageQuery = new DriverInfoPageQuery();
        driverInfoPageQuery.setDeviceId(deviceId);
        List<DriverInfo> driverInfos = driverInfoMapper.selectList(fuzzyQuery(driverInfoPageQuery));
        if (CollUtil.isEmpty(driverInfos)) {
            return Collections.emptyList();
        }
        return driverInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverInfo> list(DriverInfoPageQuery driverInfoPageQuery) {
        if (ObjectUtil.isNull(driverInfoPageQuery.getPage())) {
            driverInfoPageQuery.setPage(new Pages());
        }
        return driverInfoMapper.selectPage(driverInfoPageQuery.getPage().convert(), fuzzyQuery(driverInfoPageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<DriverInfo> fuzzyQuery(DriverInfoPageQuery driverInfoPageQuery) {
        LambdaQueryWrapper<DriverInfo> queryWrapper = Wrappers.<DriverInfo>query().lambda();
        if (ObjectUtil.isNotNull(driverInfoPageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverInfoPageQuery.getDriverAttributeId()), DriverInfo::getDriverAttributeId, driverInfoPageQuery.getDriverAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverInfoPageQuery.getDeviceId()), DriverInfo::getDeviceId, driverInfoPageQuery.getDeviceId());
        }
        return queryWrapper;
    }

}
