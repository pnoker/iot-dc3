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
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigPageQuery;
import io.github.pnoker.center.manager.mapper.PointAttributeConfigMapper;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.model.PointAttributeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointInfoService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointAttributeConfigServiceImpl implements PointAttributeConfigService {

    @Resource
    private PointAttributeConfigMapper pointAttributeConfigMapper;

    @Resource
    private PointService pointService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeConfig add(PointAttributeConfig pointAttributeConfig) {
        try {
            selectByAttributeIdAndDeviceIdAndPointId(pointAttributeConfig.getPointAttributeId(), pointAttributeConfig.getDeviceId(), pointAttributeConfig.getPointId());
            throw new DuplicateException("The point attribute config already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeConfigMapper.insert(pointAttributeConfig) > 0) {
                return pointAttributeConfigMapper.selectById(pointAttributeConfig.getId());
            }
            throw new ServiceException("The point attribute config add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return pointAttributeConfigMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeConfig update(PointAttributeConfig pointAttributeConfig) {
        PointAttributeConfig old = selectById(pointAttributeConfig.getId());
        pointAttributeConfig.setOperateTime(null);
        if (!old.getPointAttributeId().equals(pointAttributeConfig.getPointAttributeId()) || !old.getDeviceId().equals(pointAttributeConfig.getDeviceId()) || !old.getPointId().equals(pointAttributeConfig.getPointId())) {
            try {
                selectByAttributeIdAndDeviceIdAndPointId(pointAttributeConfig.getPointAttributeId(), pointAttributeConfig.getDeviceId(), pointAttributeConfig.getPointId());
                throw new DuplicateException("The point attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (pointAttributeConfigMapper.updateById(pointAttributeConfig) > 0) {
            PointAttributeConfig select = pointAttributeConfigMapper.selectById(pointAttributeConfig.getId());
            pointAttributeConfig.setPointAttributeId(select.getPointAttributeId());
            pointAttributeConfig.setDeviceId(select.getDeviceId());
            pointAttributeConfig.setPointId(select.getPointId());
            return select;
        }
        throw new ServiceException("The point attribute config update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeConfig selectById(String id) {
        PointAttributeConfig pointAttributeConfig = pointAttributeConfigMapper.selectById(id);
        if (ObjectUtil.isNull(pointAttributeConfig)) {
            throw new NotFoundException();
        }
        return pointAttributeConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeConfig selectByAttributeIdAndDeviceIdAndPointId(String pointAttributeId, String deviceId, String pointId) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        queryWrapper.eq(PointAttributeConfig::getPointAttributeId, pointAttributeId);
        queryWrapper.eq(PointAttributeConfig::getDeviceId, deviceId);
        queryWrapper.eq(PointAttributeConfig::getPointId, pointId);
        queryWrapper.last("limit 1");
        PointAttributeConfig pointAttributeConfig = pointAttributeConfigMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfig)) {
            throw new NotFoundException();
        }
        return pointAttributeConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfig> selectByAttributeId(String pointAttributeId) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        queryWrapper.eq(PointAttributeConfig::getPointAttributeId, pointAttributeId);
        List<PointAttributeConfig> pointAttributeConfigs = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigs) || pointAttributeConfigs.isEmpty()) {
            throw new NotFoundException();
        }
        return pointAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfig> selectByDeviceId(String deviceId) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        List<Point> points = pointService.selectByDeviceId(deviceId);
        Set<String> pointIds = points.stream().map(Point::getId).collect(Collectors.toSet());
        queryWrapper.eq(PointAttributeConfig::getDeviceId, deviceId);
        queryWrapper.in(PointAttributeConfig::getPointId, pointIds);
        List<PointAttributeConfig> pointAttributeConfigs = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigs)) {
            throw new NotFoundException();
        }
        return pointAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfig> selectByDeviceIdAndPointId(String deviceId, String pointId) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        queryWrapper.eq(PointAttributeConfig::getDeviceId, deviceId);
        queryWrapper.eq(PointAttributeConfig::getPointId, pointId);
        List<PointAttributeConfig> pointAttributeConfigs = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigs)) {
            throw new NotFoundException();
        }
        return pointAttributeConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointAttributeConfig> list(PointAttributeConfigPageQuery pointInfoPageQuery) {
        if (ObjectUtil.isNull(pointInfoPageQuery.getPage())) {
            pointInfoPageQuery.setPage(new Pages());
        }
        return pointAttributeConfigMapper.selectPage(pointInfoPageQuery.getPage().convert(), fuzzyQuery(pointInfoPageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<PointAttributeConfig> fuzzyQuery(PointAttributeConfigPageQuery pointInfoPageQuery) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        if (ObjectUtil.isNotNull(pointInfoPageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getPointAttributeId()), PointAttributeConfig::getPointAttributeId, pointInfoPageQuery.getPointAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getDeviceId()), PointAttributeConfig::getDeviceId, pointInfoPageQuery.getDeviceId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getPointId()), PointAttributeConfig::getPointId, pointInfoPageQuery.getPointId());
        }
        return queryWrapper;
    }

}
