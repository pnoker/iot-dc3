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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.PointInfoPageQuery;
import io.github.pnoker.center.manager.mapper.PointInfoMapper;
import io.github.pnoker.center.manager.service.PointInfoService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.model.PointInfo;
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
public class PointInfoServiceImpl implements PointInfoService {

    @Resource
    private PointInfoMapper pointInfoMapper;

    @Resource
    private PointService pointService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PointInfo add(PointInfo pointInfo) {
        try {
            selectByAttributeIdAndDeviceIdAndPointId(pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), pointInfo.getPointId());
            throw new DuplicateException("The point info already exists");
        } catch (NotFoundException notFoundException) {
            if (pointInfoMapper.insert(pointInfo) > 0) {
                return pointInfoMapper.selectById(pointInfo.getId());
            }
            throw new ServiceException("The point info add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return pointInfoMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointInfo update(PointInfo pointInfo) {
        PointInfo old = selectById(pointInfo.getId());
        pointInfo.setUpdateTime(null);
        if (!old.getPointAttributeId().equals(pointInfo.getPointAttributeId()) || !old.getDeviceId().equals(pointInfo.getDeviceId()) || !old.getPointId().equals(pointInfo.getPointId())) {
            try {
                selectByAttributeIdAndDeviceIdAndPointId(pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), pointInfo.getPointId());
                throw new DuplicateException("The point info already exists");
            } catch (NotFoundException ignored) {
                // nothing to do
            }
        }
        if (pointInfoMapper.updateById(pointInfo) > 0) {
            PointInfo select = pointInfoMapper.selectById(pointInfo.getId());
            pointInfo.setPointAttributeId(select.getPointAttributeId());
            pointInfo.setDeviceId(select.getDeviceId());
            pointInfo.setPointId(select.getPointId());
            return select;
        }
        throw new ServiceException("The point info update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointInfo selectById(String id) {
        PointInfo pointInfo = pointInfoMapper.selectById(id);
        if (null == pointInfo) {
            throw new NotFoundException();
        }
        return pointInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointInfo selectByAttributeIdAndDeviceIdAndPointId(String pointAttributeId, String deviceId, String pointId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getPointAttributeId, pointAttributeId);
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.eq(PointInfo::getPointId, pointId);
        PointInfo pointInfo = pointInfoMapper.selectOne(queryWrapper);
        if (null == pointInfo) {
            throw new NotFoundException();
        }
        return pointInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointInfo> selectByAttributeId(String pointAttributeId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getPointAttributeId, pointAttributeId);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos || pointInfos.isEmpty()) {
            throw new NotFoundException();
        }
        return pointInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointInfo> selectByDeviceId(String deviceId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        List<Point> points = pointService.selectByDeviceId(deviceId);
        Set<String> pointIds = points.stream().map(Point::getId).collect(Collectors.toSet());
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.in(PointInfo::getPointId, pointIds);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos) {
            throw new NotFoundException();
        }
        return pointInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointInfo> selectByDeviceIdAndPointId(String deviceId, String pointId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.eq(PointInfo::getPointId, pointId);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos) {
            throw new NotFoundException();
        }
        return pointInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointInfo> list(PointInfoPageQuery pointInfoPageQuery) {
        if (ObjectUtil.isNull(pointInfoPageQuery.getPage())) {
            pointInfoPageQuery.setPage(new Pages());
        }
        return pointInfoMapper.selectPage(pointInfoPageQuery.getPage().convert(), fuzzyQuery(pointInfoPageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<PointInfo> fuzzyQuery(PointInfoPageQuery pointInfoPageQuery) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        if (ObjectUtil.isNotNull(pointInfoPageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getPointAttributeId()), PointInfo::getPointAttributeId, pointInfoPageQuery.getPointAttributeId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getDeviceId()), PointInfo::getDeviceId, pointInfoPageQuery.getDeviceId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointInfoPageQuery.getPointId()), PointInfo::getPointId, pointInfoPageQuery.getPointId());
        }
        return queryWrapper;
    }

}
