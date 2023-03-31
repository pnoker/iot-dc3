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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.PointPageQuery;
import io.github.pnoker.center.manager.mapper.PointMapper;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.UnitEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Point;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Point add(Point point) {
        try {
            selectByNameAndProfileId(point.getPointName(), point.getProfileId());
            throw new DuplicateException("The point already exists in the profile");
        } catch (NotFoundException notFoundException) {
            if (pointMapper.insert(point) > 0) {
                return pointMapper.selectById(point.getId());
            }
            throw new ServiceException("The point add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return pointMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point update(Point point) {
        Point old = selectById(point.getId());
        point.setOperateTime(null);
        if (!old.getProfileId().equals(point.getProfileId()) || !old.getPointName().equals(point.getPointName())) {
            try {
                selectByNameAndProfileId(point.getPointName(), point.getProfileId());
                throw new DuplicateException("The point already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (pointMapper.updateById(point) > 0) {
            Point select = pointMapper.selectById(point.getId());
            point.setPointName(select.getPointName());
            point.setProfileId(select.getProfileId());
            return select;
        }
        throw new ServiceException("The point update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point selectById(String id) {
        Point point = pointMapper.selectById(id);
        if (ObjectUtil.isNull(point)) {
            throw new NotFoundException();
        }
        return point;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByIds(Set<String> ids) {
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
    public Point selectByNameAndProfileId(String name, String profileId) {
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
    public List<Point> selectByDeviceId(String deviceId) {
        Set<String> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
        return selectByProfileIds(profileIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Point> selectByProfileId(String profileId) {
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
    public List<Point> selectByProfileIds(Set<String> profileIds) {
        List<Point> points = new ArrayList<>(16);
        profileIds.forEach(profileId -> {
            PointPageQuery pointPageQuery = new PointPageQuery();
            pointPageQuery.setProfileId(profileId);
            List<Point> pointList = pointMapper.selectList(fuzzyQuery(pointPageQuery));
            if (ObjectUtil.isNotNull(pointList)) {
                points.addAll(pointList);
            }
        });
        if (points.isEmpty()) {
            throw new NotFoundException();
        }
        return points;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Point> list(PointPageQuery pointPageQuery) {
        if (ObjectUtil.isNull(pointPageQuery.getPage())) {
            pointPageQuery.setPage(new Pages());
        }
        return pointMapper.selectPageWithDevice(pointPageQuery.getPage().convert(), customFuzzyQuery(pointPageQuery), pointPageQuery.getDeviceId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, UnitEnum> unit(Set<String> pointIds) {
        List<Point> points = pointMapper.selectBatchIds(pointIds);
        return points.stream().collect(Collectors.toMap(Point::getId, Point::getUnit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<Point> fuzzyQuery(PointPageQuery pointPageQuery) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        if (ObjectUtil.isNotNull(pointPageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pointPageQuery.getPointName()), Point::getPointName, pointPageQuery.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointCode()), Point::getPointCode, pointPageQuery.getPointCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointTypeFlag()), Point::getPointTypeFlag, pointPageQuery.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getRwFlag()), Point::getRwFlag, pointPageQuery.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getAccrueFlag()), Point::getAccrueFlag, pointPageQuery.getAccrueFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointPageQuery.getProfileId()), Point::getProfileId, pointPageQuery.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getEnableFlag()), Point::getEnableFlag, pointPageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointPageQuery.getTenantId()), Point::getTenantId, pointPageQuery.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Point> customFuzzyQuery(PointPageQuery pointPageQuery) {
        QueryWrapper<Point> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(pointPageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pointPageQuery.getPointName()), "dp.point_name", pointPageQuery.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointCode()), "dp.point_code", pointPageQuery.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointPageQuery.getPointTypeFlag()), "dp.point_type_flag", pointPageQuery.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(pointPageQuery.getRwFlag()), "dp.rw_flag", pointPageQuery.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(pointPageQuery.getAccrueFlag()), "dp.accrue_flag", pointPageQuery.getAccrueFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointPageQuery.getProfileId()), "dp.profile_id", pointPageQuery.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotNull(pointPageQuery.getEnableFlag()), "dp.enable_flag", pointPageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointPageQuery.getTenantId()), "dp.tenant_id", pointPageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
