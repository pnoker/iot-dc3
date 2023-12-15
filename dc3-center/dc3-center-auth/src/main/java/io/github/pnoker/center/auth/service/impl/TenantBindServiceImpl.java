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

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.TenantBindBO;
import io.github.pnoker.center.auth.entity.query.TenantBindQuery;
import io.github.pnoker.center.auth.mapper.TenantBindMapper;
import io.github.pnoker.center.auth.service.TenantBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * TenantBindService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class TenantBindServiceImpl implements TenantBindService {

    @Resource
    private TenantBindMapper tenantBindMapper;

    @Override
    public void save(TenantBindBO entityBO) {
        if (tenantBindMapper.insert(entityBO) < 1) {
            throw new AddException("The tenant bind add failed");
        }
    }

    @Override
    public void remove(Long id) {
        TenantBindBO tenantBindBO = selectById(id);
        if (ObjectUtil.isNull(tenantBindBO)) {
            throw new NotFoundException("The tenant bind does not exist");
        }

        if (tenantBindMapper.deleteById(id) < 1) {
            throw new DeleteException("The tenant bind delete failed");
        }
    }

    @Override
    public void update(TenantBindBO entityBO) {
        selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (tenantBindMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The tenant bind update failed");
        }
    }

    @Override
    public TenantBindBO selectById(Long id) {
        return null;
    }

    @Override
    public TenantBindBO selectByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<TenantBindBO> wrapper = Wrappers.<TenantBindBO>query().lambda();
        wrapper.eq(TenantBindBO::getTenantId, tenantId);
        wrapper.eq(TenantBindBO::getUserId, userId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        return tenantBindMapper.selectOne(wrapper);
    }

    @Override
    public Page<TenantBindBO> selectByPage(TenantBindQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return tenantBindMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<TenantBindBO> fuzzyQuery(TenantBindQuery query) {
        LambdaQueryWrapper<TenantBindBO> wrapper = Wrappers.<TenantBindBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.eq(ObjectUtil.isNotEmpty(query.getTenantId()), TenantBindBO::getTenantId, query.getTenantId());
            wrapper.eq(ObjectUtil.isNotEmpty(query.getUserId()), TenantBindBO::getUserId, query.getUserId());
        }
        return wrapper;
    }

}
