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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigBOPageQuery;
import io.github.pnoker.center.manager.mapper.PointAttributeConfigMapper;
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.center.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.utils.PageUtil;
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
    public void save(PointAttributeConfigBO entityBO) {
        try {
            selectByAttributeIdAndDeviceIdAndPointId(entityBO.getPointAttributeId(), entityBO.getDeviceId(), entityBO.getPointId());
            throw new DuplicateException("The point attribute config already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeConfigMapper.insert(entityBO) < 1) {
                PointAttributeBO pointAttributeBO = pointAttributeMapper.selectById(entityBO.getPointAttributeId());
                throw new AddException("The point attribute config {} add failed", pointAttributeBO.getAttributeName());
            }

            // 通知驱动新增
            PointAttributeConfigBO pointAttributeConfigBO = pointAttributeConfigMapper.selectById(entityBO.getId());
            notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.ADD, pointAttributeConfigBO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        PointAttributeConfigBO pointAttributeConfigBO = selectById(id);
        if (ObjectUtil.isNull(pointAttributeConfigBO)) {
            throw new NotFoundException("The point attribute config does not exist");
        }

        if (pointAttributeConfigMapper.deleteById(id) < 1) {
            throw new DeleteException("The point attribute delete failed");
        }

        notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.DELETE, pointAttributeConfigBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(PointAttributeConfigBO entityBO) {
        PointAttributeConfigBO old = selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (!old.getPointAttributeId().equals(entityBO.getPointAttributeId()) || !old.getDeviceId().equals(entityBO.getDeviceId()) || !old.getPointId().equals(entityBO.getPointId())) {
            try {
                selectByAttributeIdAndDeviceIdAndPointId(entityBO.getPointAttributeId(), entityBO.getDeviceId(), entityBO.getPointId());
                throw new DuplicateException("The point attribute config already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }

        if (pointAttributeConfigMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The point attribute config update failed");
        }

        PointAttributeConfigBO select = pointAttributeConfigMapper.selectById(entityBO.getId());
        entityBO.setPointAttributeId(select.getPointAttributeId());
        entityBO.setDeviceId(select.getDeviceId());
        entityBO.setPointId(select.getPointId());
        notifyService.notifyDriverPointInfo(MetadataCommandTypeEnum.UPDATE, select);
    }

    @Override
    public PointAttributeConfigBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeConfigBO selectByAttributeIdAndDeviceIdAndPointId(Long pointAttributeId, Long deviceId, Long pointId) {
        LambdaQueryWrapper<PointAttributeConfigBO> queryWrapper = Wrappers.<PointAttributeConfigBO>query().lambda();
        queryWrapper.eq(PointAttributeConfigBO::getPointAttributeId, pointAttributeId);
        queryWrapper.eq(PointAttributeConfigBO::getDeviceId, deviceId);
        queryWrapper.eq(PointAttributeConfigBO::getPointId, pointId);
        queryWrapper.last("limit 1");
        PointAttributeConfigBO pointAttributeConfigBO = pointAttributeConfigMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigBO)) {
            throw new NotFoundException();
        }
        return pointAttributeConfigBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfigBO> selectByAttributeId(Long pointAttributeId) {
        LambdaQueryWrapper<PointAttributeConfigBO> queryWrapper = Wrappers.<PointAttributeConfigBO>query().lambda();
        queryWrapper.eq(PointAttributeConfigBO::getPointAttributeId, pointAttributeId);
        List<PointAttributeConfigBO> pointAttributeConfigBOS = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigBOS) || pointAttributeConfigBOS.isEmpty()) {
            throw new NotFoundException();
        }
        return pointAttributeConfigBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfigBO> selectByDeviceId(Long deviceId) {
        LambdaQueryWrapper<PointAttributeConfigBO> queryWrapper = Wrappers.<PointAttributeConfigBO>query().lambda();
        List<PointBO> pointBOS = pointService.selectByDeviceId(deviceId);
        Set<Long> pointIds = pointBOS.stream().map(PointBO::getId).collect(Collectors.toSet());
        queryWrapper.eq(PointAttributeConfigBO::getDeviceId, deviceId);
        queryWrapper.in(PointAttributeConfigBO::getPointId, pointIds);
        List<PointAttributeConfigBO> pointAttributeConfigBOS = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigBOS)) {
            throw new NotFoundException();
        }
        return pointAttributeConfigBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeConfigBO> selectByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryWrapper<PointAttributeConfigBO> queryWrapper = Wrappers.<PointAttributeConfigBO>query().lambda();
        queryWrapper.eq(PointAttributeConfigBO::getDeviceId, deviceId);
        queryWrapper.eq(PointAttributeConfigBO::getPointId, pointId);
        List<PointAttributeConfigBO> pointAttributeConfigBOS = pointAttributeConfigMapper.selectList(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeConfigBOS)) {
            throw new NotFoundException();
        }
        return pointAttributeConfigBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointAttributeConfigBO> selectByPage(PointAttributeConfigBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return pointAttributeConfigMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<PointAttributeConfigBO> fuzzyQuery(PointAttributeConfigBOPageQuery query) {
        LambdaQueryWrapper<PointAttributeConfigBO> queryWrapper = Wrappers.<PointAttributeConfigBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointAttributeId()), PointAttributeConfigBO::getPointAttributeId, query.getPointAttributeId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDeviceId()), PointAttributeConfigBO::getDeviceId, query.getDeviceId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointId()), PointAttributeConfigBO::getPointId, query.getPointId());
        }
        return queryWrapper;
    }

}
