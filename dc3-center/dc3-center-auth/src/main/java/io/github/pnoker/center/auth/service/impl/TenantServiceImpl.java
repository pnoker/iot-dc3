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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.entity.query.TenantBOPageQuery;
import io.github.pnoker.center.auth.mapper.TenantMapper;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 租户服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Override
    public void save(TenantBO entityBO) {
        TenantBO select = selectByCode(entityBO.getTenantName());
        if (ObjectUtil.isNotNull(select)) {
            throw new DuplicateException("The tenant already exists");
        }

        if (tenantMapper.insert(entityBO) < 1) {
            throw new AddException("The tenant {} add failed", entityBO.getTenantName());
        }
    }

    @Override
    public void remove(Long id) {
        TenantBO tenantBO = selectById(id);
        if (ObjectUtil.isNull(tenantBO)) {
            throw new NotFoundException("The tenant does not exist");
        }

        if (tenantMapper.deleteById(id) < 1) {
            throw new DeleteException("The tenant delete failed");
        }
    }

    @Override
    public void update(TenantBO entityBO) {
        entityBO.setTenantName(null);
        entityBO.setOperateTime(null);
        if (tenantMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The tenant update failed");
        }
    }

    @Override
    public TenantBO selectById(Long id) {
        return null;
    }

    @Override
    public TenantBO selectByCode(String code) {
        LambdaQueryWrapper<TenantBO> wrapper = Wrappers.<TenantBO>query().lambda();
        wrapper.eq(TenantBO::getTenantCode, code);
        wrapper.eq(TenantBO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        return tenantMapper.selectOne(wrapper);
    }

    @Override
    public Page<TenantBO> selectByPage(TenantBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return tenantMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<TenantBO> fuzzyQuery(TenantBOPageQuery query) {
        LambdaQueryWrapper<TenantBO> wrapper = Wrappers.<TenantBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getTenantName()), TenantBO::getTenantName, query.getTenantName());
        }
        return wrapper;
    }

}
