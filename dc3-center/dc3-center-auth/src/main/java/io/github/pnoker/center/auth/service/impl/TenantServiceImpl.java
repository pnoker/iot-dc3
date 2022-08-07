/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.mapper.TenantMapper;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.TenantDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Tenant;
import lombok.extern.slf4j.Slf4j;
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
    public Tenant add(Tenant tenant) {
        Tenant select = selectByName(tenant.getName());
        if (ObjectUtil.isNotNull(select)) {
            throw new DuplicateException("The tenant already exists");
        }
        if (tenantMapper.insert(tenant) > 0) {
            return tenantMapper.selectById(tenant.getId());
        }
        throw new ServiceException("The tenant add failed");
    }

    @Override
    public boolean delete(String id) {
        Tenant tenant = selectById(id);
        if (null == tenant) {
            throw new NotFoundException("The tenant does not exist");
        }
        return tenantMapper.deleteById(id) > 0;
    }

    @Override
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
    public Tenant selectById(String id) {
        return tenantMapper.selectById(id);
    }

    @Override
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
    public Page<Tenant> list(TenantDto tenantDto) {
        if (ObjectUtil.isNull(tenantDto.getPage())) {
            tenantDto.setPage(new Pages());
        }
        return tenantMapper.selectPage(tenantDto.getPage().convert(), fuzzyQuery(tenantDto));
    }

    @Override
    public LambdaQueryWrapper<Tenant> fuzzyQuery(TenantDto tenantDto) {
        LambdaQueryWrapper<Tenant> queryWrapper = Wrappers.<Tenant>query().lambda();
        if (ObjectUtil.isNotNull(tenantDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(tenantDto.getName()), Tenant::getName, tenantDto.getName());
        }
        return queryWrapper;
    }

}
