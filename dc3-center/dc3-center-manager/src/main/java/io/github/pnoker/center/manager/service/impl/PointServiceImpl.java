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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.dto.PointDto;
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
        point.setUpdateTime(null);
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
        if (null == point) {
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
        Point point = pointMapper.selectOne(queryWrapper);
        if (null == point) {
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
        PointDto pointDto = new PointDto();
        pointDto.setProfileId(profileId);
        List<Point> points = pointMapper.selectList(fuzzyQuery(pointDto));
        if (null == points || points.isEmpty()) {
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
            PointDto pointDto = new PointDto();
            pointDto.setProfileId(profileId);
            List<Point> pointList = pointMapper.selectList(fuzzyQuery(pointDto));
            if (null != pointList) {
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
    public Page<Point> list(PointDto pointDto) {
        if (ObjectUtil.isNull(pointDto.getPage())) {
            pointDto.setPage(new Pages());
        }
        return pointMapper.selectPageWithDevice(pointDto.getPage().convert(), customFuzzyQuery(pointDto), pointDto.getDeviceId());
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
    public LambdaQueryWrapper<Point> fuzzyQuery(PointDto pointDto) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        if (null != pointDto) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(pointDto.getPointName()), Point::getPointName, pointDto.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointDto.getPointTypeFlag()), Point::getPointTypeFlag, pointDto.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointDto.getRwFlag()), Point::getRwFlag, pointDto.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointDto.getAccrueFlag()), Point::getAccrueFlag, pointDto.getAccrueFlag());
            queryWrapper.eq(CharSequenceUtil.isNotBlank(pointDto.getProfileId()), Point::getProfileId, pointDto.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointDto.getEnableFlag()), Point::getEnableFlag, pointDto.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotBlank(pointDto.getTenantId()), Point::getTenantId, pointDto.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Point> customFuzzyQuery(PointDto pointDto) {
        QueryWrapper<Point> queryWrapper = Wrappers.query();
        queryWrapper.eq("dp.deleted", 0);
        if (ObjectUtil.isNotNull(pointDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(pointDto.getPointName()), "dp.name", pointDto.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pointDto.getPointTypeFlag()), "dp.type", pointDto.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(pointDto.getRwFlag()), "dp.rw", pointDto.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotNull(pointDto.getAccrueFlag()), "dp.accrue", pointDto.getAccrueFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointDto.getProfileId()), "dp.profile_id", pointDto.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotNull(pointDto.getEnableFlag()), "dp.enable", pointDto.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointDto.getTenantId()), "dp.tenant_id", pointDto.getTenantId());
        }
        return queryWrapper.lambda();
    }

}
