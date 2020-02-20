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

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.manager.mapper.PointAttributeMapper;
import com.pnoker.center.manager.service.DriverService;
import com.pnoker.center.manager.service.PointAttributeService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.PointAttributeDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>模板配置信息服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointAttributeServiceImpl implements PointAttributeService {

    @Resource
    private DriverService driverService;

    @Resource
    private PointAttributeMapper pointAttributeMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#pointAttribute.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#pointAttribute.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointAttribute add(PointAttribute pointAttribute) {
        PointAttribute select = selectByName(pointAttribute.getName());
        if (null != select) {
            throw new ServiceException("profile info already exists");
        }
        if (pointAttributeMapper.insert(pointAttribute) > 0) {
            return pointAttributeMapper.selectById(pointAttribute.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return pointAttributeMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#pointAttribute.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#pointAttribute.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointAttribute update(PointAttribute pointAttribute) {
        pointAttribute.setUpdateTime(null);
        if (pointAttributeMapper.updateById(pointAttribute) > 0) {
            PointAttribute select = selectById(pointAttribute.getId());
            pointAttribute.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public PointAttribute selectById(Long id) {
        return pointAttributeMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public PointAttribute selectByName(String name) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        queryWrapper.like(PointAttribute::getName, name);
        return pointAttributeMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<PointAttribute> list(PointAttributeDto profileInfoDto) {
        if (!Optional.ofNullable(profileInfoDto.getPage()).isPresent()) {
            profileInfoDto.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(profileInfoDto.getPage().convert(), fuzzyQuery(profileInfoDto));
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, key = "'profile_info_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        List<Dic> driverDicList = driverService.dictionary();
        for (Dic driverDic : driverDicList) {
            List<Dic> dicList = new ArrayList<>();
            LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
            queryWrapper.eq(PointAttribute::getDriverId, driverDic.getValue());
            List<PointAttribute> pointAttributeList = pointAttributeMapper.selectList(queryWrapper);
            driverDic.setDisabled(true);
            driverDic.setValue(RandomUtil.randomLong());
            for (PointAttribute pointAttribute : pointAttributeList) {
                Dic profileInfoDic = new Dic().setLabel(pointAttribute.getDisplayName()).setValue(pointAttribute.getId());
                dicList.add(profileInfoDic);
            }
            driverDic.setChildren(dicList);
        }
        return driverDicList;
    }

    @Override
    public LambdaQueryWrapper<PointAttribute> fuzzyQuery(PointAttributeDto profileInfoDto) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        Optional.ofNullable(profileInfoDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(PointAttribute::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
