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
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointBOPageQuery;
import io.github.pnoker.center.manager.mapper.PointMapper;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
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
    public void save(PointBO entityBO) {
        try {
            selectByNameAndProfileId(entityBO.getPointName(), entityBO.getProfileId());
            throw new DuplicateException("The point already exists in the profile");
        } catch (NotFoundException notFoundException) {
            if (pointMapper.insert(entityBO) < 1) {
                throw new AddException("The point {} add failed", entityBO.getPointName());
            }

            // 通知驱动新增
            PointBO pointBO = pointMapper.selectById(entityBO.getId());
            notifyService.notifyDriverPoint(MetadataCommandTypeEnum.ADD, pointBO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        PointBO pointBO = selectById(id);
        if (ObjectUtil.isNull(pointBO)) {
            throw new NotFoundException("The point does not exist");
        }

        if (pointMapper.deleteById(id) < 1) {
            throw new DeleteException("The point delete failed");
        }

        notifyService.notifyDriverPoint(MetadataCommandTypeEnum.DELETE, pointBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(PointBO entityBO) {
        PointBO old = selectById(entityBO.getId());
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

        PointBO select = pointMapper.selectById(entityBO.getId());
        entityBO.setPointName(select.getPointName());
        entityBO.setProfileId(select.getProfileId());
        notifyService.notifyDriverPoint(MetadataCommandTypeEnum.UPDATE, select);
    }

    @Override
    public PointBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointBO> selectByIds(Set<Long> ids) {
        List<PointBO> devices = pointMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(devices)) {
            throw new NotFoundException();
        }
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointBO selectByNameAndProfileId(String name, Long profileId) {
        LambdaQueryWrapper<PointBO> queryWrapper = Wrappers.<PointBO>query().lambda();
        queryWrapper.eq(PointBO::getPointName, name);
        queryWrapper.eq(PointBO::getProfileId, profileId);
        queryWrapper.last("limit 1");
        PointBO pointBO = pointMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(pointBO)) {
            throw new NotFoundException();
        }
        return pointBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointBO> selectByDeviceId(Long deviceId) {
        Set<Long> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
        return selectByProfileIds(profileIds, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointBO> selectByProfileId(Long profileId) {
        PointBOPageQuery pointPageQuery = new PointBOPageQuery();
        pointPageQuery.setProfileId(profileId);
        List<PointBO> pointBOS = pointMapper.selectList(fuzzyQuery(pointPageQuery));
        if (ObjectUtil.isNull(pointBOS) || pointBOS.isEmpty()) {
            throw new NotFoundException();
        }
        return pointBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointBO> selectByProfileIds(Set<Long> profileIds, boolean throwException) {
        List<PointBO> pointBOS = new ArrayList<>(16);
        profileIds.forEach(profileId -> {
            PointBOPageQuery pointPageQuery = new PointBOPageQuery();
            pointPageQuery.setProfileId(profileId);
            List<PointBO> pointBOList = pointMapper.selectList(fuzzyQuery(pointPageQuery));
            if (ObjectUtil.isNotNull(pointBOList)) {
                pointBOS.addAll(pointBOList);
            }
        });
        if (throwException) {
            if (pointBOS.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return pointBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointBO> selectByPage(PointBOPageQuery entityQuery) {
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
        List<PointBO> pointBOS = pointMapper.selectBatchIds(pointIds);
        return pointBOS.stream().collect(Collectors.toMap(PointBO::getId, PointBO::getUnit));
    }

    @Override
    public Long count() {
        return pointMapper.selectCount(new QueryWrapper<>());
    }

    private LambdaQueryWrapper<PointBO> fuzzyQuery(PointBOPageQuery query) {
        LambdaQueryWrapper<PointBO> queryWrapper = Wrappers.<PointBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getPointName()), PointBO::getPointName, query.getPointName());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointCode()), PointBO::getPointCode, query.getPointCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getPointTypeFlag()), PointBO::getPointTypeFlag, query.getPointTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getRwFlag()), PointBO::getRwFlag, query.getRwFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getProfileId()), PointBO::getProfileId, query.getProfileId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getEnableFlag()), PointBO::getEnableFlag, query.getEnableFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getTenantId()), PointBO::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<PointBO> customFuzzyQuery(PointBOPageQuery pointPageQuery) {
        QueryWrapper<PointBO> queryWrapper = Wrappers.query();
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
