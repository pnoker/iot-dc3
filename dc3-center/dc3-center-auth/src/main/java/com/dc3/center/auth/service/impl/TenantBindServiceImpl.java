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

package com.dc3.center.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.auth.mapper.TenantBindMapper;
import com.dc3.center.auth.service.TenantBindService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.dto.TenantBindDto;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.TenantBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * TenantBindService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class TenantBindServiceImpl implements TenantBindService {
    @Resource
    private TenantBindMapper tenantBindMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.ID, key = "#tenantBind.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.TENANT_ID + CacheConstant.Suffix.USER_ID, key = "#tenantBind.tenantId+'.'+#tenantBind.userId+'.'+#tenantBind.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public TenantBind add(TenantBind tenantBind) {
        if (tenantBindMapper.insert(tenantBind) > 0) {
            return tenantBindMapper.selectById(tenantBind.getId());
        }
        throw new ServiceException("The tenant bind add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.TENANT_ID + CacheConstant.Suffix.USER_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(String id) {
        selectById(id);
        return tenantBindMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.ID, key = "#tenantBind.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.TENANT_ID + CacheConstant.Suffix.USER_ID, key = "#tenantBind.tenantId+'.'+#tenantBind.userId+'.'+#tenantBind.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public TenantBind update(TenantBind tenantBind) {
        selectById(tenantBind.getId());
        tenantBind.setUpdateTime(null);
        if (tenantBindMapper.updateById(tenantBind) > 0) {
            return tenantBindMapper.selectById(tenantBind.getId());
        }
        throw new ServiceException("The tenant bind update failed");
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.ID, key = "#id", unless = "#result==null")
    public TenantBind selectById(String id) {
        TenantBind tenantBind = tenantBindMapper.selectById(id);
        if (null == tenantBind) {
            throw new NotFoundException("The tenant bind does not exist");
        }
        return tenantBind;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.TENANT_ID + CacheConstant.Suffix.USER_ID, key = "#tenantId+'.'+#userId", unless = "#result==null")
    public TenantBind selectByTenantIdAndUserId(String tenantId, String userId) {
        LambdaQueryWrapper<TenantBind> queryWrapper = Wrappers.<TenantBind>query().lambda();
        queryWrapper.eq(TenantBind::getTenantId, tenantId);
        queryWrapper.eq(TenantBind::getUserId, userId);
        TenantBind tenantBind = tenantBindMapper.selectOne(queryWrapper);
        if (null == tenantBind) {
            throw new NotFoundException("The tenant bind does not exist");
        }
        return tenantBind;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.TENANT_BIND + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<TenantBind> list(TenantBindDto tenantBindDto) {
        if (!Optional.ofNullable(tenantBindDto.getPage()).isPresent()) {
            tenantBindDto.setPage(new Pages());
        }
        return tenantBindMapper.selectPage(tenantBindDto.getPage().convert(), fuzzyQuery(tenantBindDto));
    }

    @Override
    public LambdaQueryWrapper<TenantBind> fuzzyQuery(TenantBindDto tenantBindDto) {
        LambdaQueryWrapper<TenantBind> queryWrapper = Wrappers.<TenantBind>query().lambda();
        if (null != tenantBindDto) {
            if (null != tenantBindDto.getTenantId()) {
                queryWrapper.eq(TenantBind::getTenantId, tenantBindDto.getTenantId());
            }
            if (null != tenantBindDto.getUserId()) {
                queryWrapper.eq(TenantBind::getUserId, tenantBindDto.getUserId());
            }
        }
        return queryWrapper;
    }

}
