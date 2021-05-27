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
import com.dc3.center.auth.mapper.TenantBindMapper;
import com.dc3.center.auth.service.TenantBindService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
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
 * <p>TenantBindService Impl
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
                    @CachePut(value = Common.Cache.TENANT_BIND + Common.Cache.ID, key = "#tenantBind.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.TENANT_BIND + Common.Cache.TENANT_ID + Common.Cache.ENTITY_ID, key = "#tenantBind.tenantId+'.'+#tenantBind.entityId+'.'+#tenantBind.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
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
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.TENANT_ID + Common.Cache.ENTITY_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return tenantBindMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.TENANT_BIND + Common.Cache.ID, key = "#tenantBind.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.TENANT_BIND + Common.Cache.TENANT_ID + Common.Cache.ENTITY_ID, key = "#tenantBind.tenantId+'.'+#tenantBind.entityId+'.'+#tenantBind.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.TENANT_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
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
    @Cacheable(value = Common.Cache.TENANT_BIND + Common.Cache.ID, key = "#id", unless = "#result==null")
    public TenantBind selectById(Long id) {
        TenantBind tenantBind = tenantBindMapper.selectById(id);
        if (null == tenantBind) {
            throw new NotFoundException("The tenant bind does not exist");
        }
        return tenantBind;
    }

    @Override
    @Cacheable(value = Common.Cache.TENANT_BIND + Common.Cache.TENANT_ID + Common.Cache.ENTITY_ID, key = "#tenantId+'.'+#entityId+'.'+#type", unless = "#result==null")
    public TenantBind selectByTenantIdAndEntityId(Long tenantId, Long entityId, String type) {
        LambdaQueryWrapper<TenantBind> queryWrapper = Wrappers.<TenantBind>query().lambda();
        queryWrapper.eq(TenantBind::getTenantId, tenantId);
        queryWrapper.eq(TenantBind::getEntityId, entityId);
        queryWrapper.eq(TenantBind::getType, type);
        TenantBind tenantBind = tenantBindMapper.selectOne(queryWrapper);
        if (null == tenantBind) {
            throw new NotFoundException("The tenant bind does not exist");
        }
        return tenantBind;
    }

    @Override
    @Cacheable(value = Common.Cache.TENANT_BIND + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
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
            if (null != tenantBindDto.getEntityId()) {
                queryWrapper.eq(TenantBind::getEntityId, tenantBindDto.getEntityId());
            }
            if (StrUtil.isNotBlank(tenantBindDto.getType())) {
                queryWrapper.eq(TenantBind::getType, tenantBindDto.getType());
            }
        }
        return queryWrapper;
    }

}
