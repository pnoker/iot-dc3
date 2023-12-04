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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.PointPageQuery;
import io.github.pnoker.center.manager.mapper.PointMapper;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointServiceImpl implements PointService {

    @Resource
    private PointMapper pointMapper;

    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private NotifyService notifyService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(Point entityBO) {
        try {
            selectByNameAndProfileId(entityBO.getPointName(), entityBO.getProfileId());
            throw new DuplicateException("The point already exists in the profile");
        } catch (NotFoundException notFoundException) {
            if (pointMapper.insert(entityBO) < 1) {
                throw new AddException("The point {} add failed", entityBO.getPointName());
            }

            // 通知驱动新增
            Point point = pointMapper.selectById(entityBO.getId());
            notifyService.notifyDriverPoint(MetadataCommandTypeEnum.ADD, point);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        Point point = selectById(id);
        if (ObjectUtil.isNull(point)) {
            throw new NotFoundException("The point does not exist");
        }

        if (pointMapper.deleteById(id) < 1) {
            throw new DeleteException("The point delete failed");
        }

        notifyService.notifyDriverPoint(MetadataCommandTypeEnum.DELETE, point);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Point entityBO) {
        Point old = selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (!old.getProfileId().equals(entityBO.getProfileId()) || !old.getPointName().equals(entityBO.getPointName())) {
            try {
                selectByNameAndProfileId(entityBO.getPointName(), entityBO.getProfileId());
                throw new DuplicateException("The point already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }

        if (pointMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The point update failed");
        }

        Point select = pointMapper.selectById(entityBO.getId());
        entityBO.setPointName(select.getPointName());
        entityBO.setProfileId(select.getProfileId());
        notifyService.notifyDriverPoint(MetadataCommandTypeEnum.UPDATE, select);
    }

    @Override
    public Point selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByIds(Set<Long> ids) {
        List<Point> devices = pointMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(devices)) {
            throw new NotFoundException();
        }
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point selectByNameAndProfileId(String name, Long profileId) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        queryWrapper.eq(Point::getPointName, name);
        queryWrapper.eq(Point::getProfileId, profileId);
        queryWrapper.last("limit 1");
        Point point = pointMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(point)) {
            throw new NotFoundException();
        }
        return point;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByDeviceId(Long deviceId) {
        Set<Long> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
        return selectByProfileIds(profileIds, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByProfileId(Long profileId) {
        PointPageQuery pointPageQuery = new PointPageQuery();
        pointPageQuery.setProfileId(profileId);
        List<Point> points = pointMapper.selectList(fuzzyQuery(pointPageQuery));
        if (ObjectUtil.isNull(points) || points.isEmpty()) {
            throw new NotFoundException();
        }
        return points;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByProfileIds(Set<Long> profileIds, boolean throwException) {
        List<Point> points = new ArrayList<>(16);
        profileIds.forEach(profileId -> {
            PointPageQuery pointPageQuery = new PointPageQuery();
            pointPageQuery.setProfileId(profileId);
            List<Point> pointList = pointMapper.selectList(fuzzyQuery(pointPageQuery));
            if (ObjectUtil.isNotNull(pointList)) {
                points.addAll(pointList);
            }
        });
        if (throwException) {
            if (points.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return points;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Point> selectByPage(PointPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return pointMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()), customFuzzyQuery(entityQuery), entityQuery.getDeviceId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, String> unit(Set<Long> pointIds) {
        List<Point> points = pointMapper.selectBatchIds(pointIds);
        return points.stream().collect(Collectors.toMap(Point::getId, Point::getUnit));
    }

    @Override
    public Long count() {
        return pointMapper.selectCount(new QueryWrapper<>());
    }

    private LambdaQueryWrapper<Point> fuzzyQuery(PointPageQuery query) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getPointName()), Point::getPointName, query.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointCode()), Point::getPointCode, query.getPointCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointTypeFlag()), Point::getPointTypeFlag, query.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getRwFlag()), Point::getRwFlag, query.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getProfileId()), Point::getProfileId, query.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getEnableFlag()), Point::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getTenantId()), Point::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<Point> customFuzzyQuery(PointPageQuery pointPageQuery) {
        QueryWrapper<Point> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(pointPageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pointPageQuery.getPointName()), "dp.point_name", pointPageQuery.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointCode()), "dp.point_code", pointPageQuery.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointTypeFlag()), "dp.point_type_flag", pointPageQuery.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(pointPageQuery.getRwFlag()), "dp.rw_flag", pointPageQuery.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getProfileId()), "dp.profile_id", pointPageQuery.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotNull(pointPageQuery.getEnableFlag()), "dp.enable_flag", pointPageQuery.getEnableFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getTenantId()), "dp.tenant_id", pointPageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
