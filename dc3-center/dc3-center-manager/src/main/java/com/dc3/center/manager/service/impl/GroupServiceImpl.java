/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.GroupMapper;
import com.dc3.center.manager.service.GroupService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.dto.GroupDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * GroupService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    private GroupMapper groupMapper;


    //todo 分组逻辑需要调整，同时支持驱动、模版、位号、设备，分组只是一种UI上的显示逻辑，不影响实际数据采集
    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.ID, key = "#group.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.NAME, key = "#group.name+'.'+#group.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Group add(Group group) {
        try {
            selectByName(group.getName(), group.getTenantId());
            throw new DuplicateException("The device group already exists");
        } catch (NotFoundException notFoundException) {
            if (groupMapper.insert(group) > 0) {
                return groupMapper.selectById(group.getId());
            }
            throw new ServiceException("The group add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(String id) {
        selectById(id);
        return groupMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.ID, key = "#group.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.NAME, key = "#group.name+'.'+#group.tenantId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Group update(Group group) {
        selectById(group.getId());
        group.setUpdateTime(null);
        if (groupMapper.updateById(group) > 0) {
            Group select = groupMapper.selectById(group.getId());
            group.setName(select.getName());
            return select;
        }
        throw new ServiceException("The group update failed");
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.ID, key = "#id", unless = "#result==null")
    public Group selectById(String id) {
        Group group = groupMapper.selectById(id);
        if (null == group) {
            throw new NotFoundException("The group does not exist");
        }
        return group;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.NAME, key = "#name+'.'+#tenantId", unless = "#result==null")
    public Group selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        queryWrapper.eq(Group::getName, name);
        Group group = groupMapper.selectOne(queryWrapper);
        if (null == group) {
            throw new NotFoundException("The group does not exist");
        }
        return group;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.GROUP + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Group> list(GroupDto groupDto) {
        if (!Optional.ofNullable(groupDto.getPage()).isPresent()) {
            groupDto.setPage(new Pages());
        }
        return groupMapper.selectPage(groupDto.getPage().convert(), fuzzyQuery(groupDto));
    }

    @Override
    public LambdaQueryWrapper<Group> fuzzyQuery(GroupDto groupDto) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        if (null != groupDto) {
            if (StrUtil.isNotBlank(groupDto.getName())) {
                queryWrapper.like(Group::getName, groupDto.getName());
            }
            if (null != groupDto.getTenantId()) {
                queryWrapper.eq(Group::getTenantId, groupDto.getTenantId());
            }
        }
        return queryWrapper;
    }

}
