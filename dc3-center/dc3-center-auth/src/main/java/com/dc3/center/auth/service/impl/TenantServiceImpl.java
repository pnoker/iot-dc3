/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
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

package com.dc3.center.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.auth.mapper.TenantMapper;
import com.dc3.center.auth.service.TenantService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.TenantDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * 租户服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.TENANT + Common.Cache.ID, key = "#tenant.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.TENANT + Common.Cache.NAME, key = "#tenant.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Tenant add(Tenant tenant) {
        Tenant select = selectByName(tenant.getName());
        if (null != select) {
            throw new DuplicateException("The tenant already exists");
        }
        if (tenantMapper.insert(tenant) > 0) {
            return tenantMapper.selectById(tenant.getId());
        }
        throw new ServiceException("The tenant add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        Tenant tenant = selectById(id);
        if (null == tenant) {
            throw new NotFoundException("The tenant does not exist");
        }
        return tenantMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.TENANT + Common.Cache.ID, key = "#tenant.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.TENANT + Common.Cache.NAME, key = "#tenant.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.TENANT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Tenant update(Tenant tenant) {
        tenant.setName(null).setUpdateTime(null);
        if (tenantMapper.updateById(tenant) > 0) {
            Tenant select = tenantMapper.selectById(tenant.getId());
            tenant.setName(select.getName());
            return select;
        }
        throw new ServiceException("The tenant update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.TENANT + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Tenant selectById(Long id) {
        return tenantMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.TENANT + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public Tenant selectByName(String name) {
        LambdaQueryWrapper<Tenant> queryWrapper = Wrappers.<Tenant>query().lambda();
        queryWrapper.eq(Tenant::getName, name);
        Tenant tenant = tenantMapper.selectOne(queryWrapper);
        if (null == tenant) {
            throw new NotFoundException("The tenant does not exist");
        }
        return tenant;
    }

    @Override
    @Cacheable(value = Common.Cache.TENANT + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Tenant> list(TenantDto tenantDto) {
        if (!Optional.ofNullable(tenantDto.getPage()).isPresent()) {
            tenantDto.setPage(new Pages());
        }
        return tenantMapper.selectPage(tenantDto.getPage().convert(), fuzzyQuery(tenantDto));
    }

    @Override
    public LambdaQueryWrapper<Tenant> fuzzyQuery(TenantDto tenantDto) {
        LambdaQueryWrapper<Tenant> queryWrapper = Wrappers.<Tenant>query().lambda();
        if (null != tenantDto) {
            if (StrUtil.isNotBlank(tenantDto.getName())) {
                queryWrapper.like(Tenant::getName, tenantDto.getName());
            }
        }
        return queryWrapper;
    }

}
