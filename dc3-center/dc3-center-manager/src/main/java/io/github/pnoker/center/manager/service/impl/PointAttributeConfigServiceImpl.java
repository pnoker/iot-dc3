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
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.model.PointAttribute;
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
    private PointAttributeMapper pointAttributeMapper;
    @Resource
    private PointAttributeConfigMapper pointAttributeConfigMapper;

    @Resource
    private PointService pointService;
    @Resource
    private NotifyService notifyService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(PointAttributeConfig entityDO) {
        try {
            selectByAttributeIdAndDeviceIdAndPointId(entityDO.getPointAttributeId(), entityDO.getDeviceId(), entityDO.getPointId());
            throw new DuplicateException("The point attribute config already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeConfigMapper.insert(entityDO) < 1) {
                PointAttribute pointAttribute = pointAttributeMapper.selectById(entityDO.getPointAttributeId());
                throw new AddException("The point attribute config {} add failed", pointAttribute.getAttributeName());
            }

            // 通知驱动新增
            PointAttributeConfig pointAttributeConfig = pointAttributeConfigMapper.selectById(entityDO.getId());
            notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.ADD, pointAttributeConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        PointAttributeConfig pointAttributeConfig = selectById(id);
        if (ObjectUtil.isNull(pointAttributeConfig)) {
            throw new NotFoundException("The point attribute config does not exist");
        }

        if (pointAttributeConfigMapper.deleteById(id) < 1) {
            throw new DeleteException("The point attribute delete failed");
        }

        notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.DELETE, pointAttributeConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(PointAttributeConfig entityDO) {
        PointAttributeConfig old = selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (!old.getPointAttributeId().equals(entityDO.getPointAttributeId()) || !old.getDeviceId().equals(entityDO.getDeviceId()) || !old.getPointId().equals(entityDO.getPointId())) {
            try {
                selectByAttributeIdAndDeviceIdAndPointId(entityDO.getPointAttributeId(), entityDO.getDeviceId(), entityDO.getPointId());
                throw new DuplicateException("The point attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }

        if (pointAttributeConfigMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The point attribute config update failed");
        }

        PointAttributeConfig select = pointAttributeConfigMapper.selectById(entityDO.getId());
        entityDO.setPointAttributeId(select.getPointAttributeId());
        entityDO.setDeviceId(select.getDeviceId());
        entityDO.setPointId(select.getPointId());
        notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.UPDATE, select);
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
    public Page<PointAttributeConfig> list(PointAttributeConfigPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return pointAttributeConfigMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<PointAttributeConfig> fuzzyQuery(PointAttributeConfigPageQuery query) {
        LambdaQueryWrapper<PointAttributeConfig> queryWrapper = Wrappers.<PointAttributeConfig>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getPointAttributeId()), PointAttributeConfig::getPointAttributeId, query.getPointAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDeviceId()), PointAttributeConfig::getDeviceId, query.getDeviceId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getPointId()), PointAttributeConfig::getPointId, query.getPointId());
        }
        return queryWrapper;
    }

}
