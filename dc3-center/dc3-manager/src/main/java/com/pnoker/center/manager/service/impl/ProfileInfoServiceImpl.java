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
import com.pnoker.center.manager.mapper.ProfileInfoMapper;
import com.pnoker.center.manager.service.DriverService;
import com.pnoker.center.manager.service.ProfileInfoService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ProfileInfoDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.ProfileInfo;
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
public class ProfileInfoServiceImpl implements ProfileInfoService {

    @Resource
    private DriverService driverService;

    @Resource
    private ProfileInfoMapper profileInfoMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE_INFO + Common.Cache.ID, key = "#profileInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE_INFO + Common.Cache.NAME, key = "#profileInfo.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public ProfileInfo add(ProfileInfo profileInfo) {
        ProfileInfo select = selectByName(profileInfo.getName());
        if (null != select) {
            throw new ServiceException("profile info already exists");
        }
        if (profileInfoMapper.insert(profileInfo) > 0) {
            return profileInfoMapper.selectById(profileInfo.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return profileInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.PROFILE_INFO + Common.Cache.ID, key = "#profileInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.PROFILE_INFO + Common.Cache.NAME, key = "#profileInfo.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.PROFILE_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public ProfileInfo update(ProfileInfo profileInfo) {
        profileInfo.setUpdateTime(null);
        if (profileInfoMapper.updateById(profileInfo) > 0) {
            ProfileInfo select = selectById(profileInfo.getId());
            profileInfo.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_INFO + Common.Cache.ID, key = "#id", unless = "#result==null")
    public ProfileInfo selectById(Long id) {
        return profileInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_INFO + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public ProfileInfo selectByName(String name) {
        LambdaQueryWrapper<ProfileInfo> queryWrapper = Wrappers.<ProfileInfo>query().lambda();
        queryWrapper.like(ProfileInfo::getName, name);
        return profileInfoMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_INFO + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<ProfileInfo> list(ProfileInfoDto profileInfoDto) {
        if (!Optional.ofNullable(profileInfoDto.getPage()).isPresent()) {
            profileInfoDto.setPage(new Pages());
        }
        return profileInfoMapper.selectPage(profileInfoDto.getPage().convert(), fuzzyQuery(profileInfoDto));
    }

    @Override
    @Cacheable(value = Common.Cache.PROFILE_INFO + Common.Cache.DIC, key = "'profile_info_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        List<Dic> driverDicList = driverService.dictionary();
        for (Dic driverDic : driverDicList) {
            List<Dic> dicList = new ArrayList<>();
            LambdaQueryWrapper<ProfileInfo> queryWrapper = Wrappers.<ProfileInfo>query().lambda();
            queryWrapper.eq(ProfileInfo::getDriverId, driverDic.getValue());
            List<ProfileInfo> profileInfoList = profileInfoMapper.selectList(queryWrapper);
            driverDic.setDisabled(true);
            driverDic.setValue(RandomUtil.randomLong());
            for (ProfileInfo profileInfo : profileInfoList) {
                Dic profileInfoDic = new Dic().setLabel(profileInfo.getDisplayName()).setValue(profileInfo.getId());
                dicList.add(profileInfoDic);
            }
            driverDic.setChildren(dicList);
        }
        return driverDicList;
    }

    @Override
    public LambdaQueryWrapper<ProfileInfo> fuzzyQuery(ProfileInfoDto profileInfoDto) {
        LambdaQueryWrapper<ProfileInfo> queryWrapper = Wrappers.<ProfileInfo>query().lambda();
        Optional.ofNullable(profileInfoDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(ProfileInfo::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
