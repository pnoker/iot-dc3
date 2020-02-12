/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.manager.mapper.PointInfoMapper;
import com.pnoker.center.manager.service.PointInfoService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.PointInfoDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.PointInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * <p>位号配置信息服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointInfoServiceImpl implements PointInfoService {

    @Resource
    private PointInfoMapper pointInfoMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_INFO + Common.Cache.ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_INFO + Common.Cache.PROFILE_INFO_ID, key = "#pointInfo.profileInfoId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointInfo add(PointInfo pointInfo) {
        PointInfo select = selectByProfileInfoID(pointInfo.getProfileInfoId());
        if (null != select) {
            throw new ServiceException("point info already exists");
        }
        if (pointInfoMapper.insert(pointInfo) > 0) {
            return pointInfoMapper.selectById(pointInfo.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.PROFILE_INFO_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return pointInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_INFO + Common.Cache.ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_INFO + Common.Cache.PROFILE_INFO_ID, key = "#pointInfo.profileInfoId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointInfo update(PointInfo pointInfo) {
        pointInfo.setUpdateTime(null);
        if (pointInfoMapper.updateById(pointInfo) > 0) {
            PointInfo select = selectById(pointInfo.getId());
            pointInfo.setProfileInfoId(select.getProfileInfoId());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO + Common.Cache.ID, key = "#id", unless = "#result==null")
    public PointInfo selectById(Long id) {
        return pointInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO + Common.Cache.PROFILE_INFO_ID, key = "#id", unless = "#result==null")
    public PointInfo selectByProfileInfoID(Long id) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.like(PointInfo::getProfileInfoId, id);
        return pointInfoMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<PointInfo> list(PointInfoDto pointInfoDto) {
        if (!Optional.ofNullable(pointInfoDto.getPage()).isPresent()) {
            pointInfoDto.setPage(new Pages());
        }
        return pointInfoMapper.selectPage(pointInfoDto.getPage().convert(), fuzzyQuery(pointInfoDto));
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO + Common.Cache.DIC, key = "'point_info_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        /*LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        return pointInfoMapper.selectList(queryWrapper);*/
        return null;
    }

    @Override
    public LambdaQueryWrapper<PointInfo> fuzzyQuery(PointInfoDto pointInfoDto) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        Optional.ofNullable(pointInfoDto).ifPresent(dto -> {
            if (null != dto.getProfileInfoId()) {
                queryWrapper.eq(PointInfo::getProfileInfoId, dto.getProfileInfoId());
            }
            if (null != dto.getDeviceId()) {
                queryWrapper.eq(PointInfo::getDeviceId, dto.getDeviceId());
            }
            if (null != dto.getPointId()) {
                queryWrapper.eq(PointInfo::getPointId, dto.getPointId());
            }
        });
        return queryWrapper;
    }

}
