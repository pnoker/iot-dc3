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
import io.github.pnoker.center.auth.dal.TenantManager;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.entity.builder.TenantBuilder;
import io.github.pnoker.center.auth.entity.model.TenantDO;
import io.github.pnoker.center.auth.entity.query.TenantQuery;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
    private TenantBuilder tenantBuilder;

    @Resource
    private TenantManager tenantManager;

    @Override
    public void save(TenantBO entityBO) {
        checkDuplicate(entityBO, false, true);

        TenantDO entityDO = tenantBuilder.buildDOByBO(entityBO);
        if (!tenantManager.save(entityDO)) {
            throw new AddException("The tenant {} add failed", entityBO.getTenantName());
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!tenantManager.removeById(id)) {
            throw new DeleteException("The tenant delete failed");
        }
    }

    @Override
    public void update(TenantBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        TenantDO entityDO = tenantBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!tenantManager.updateById(entityDO)) {
            throw new UpdateException("租户更新失败");
        }
    }

    @Override
    public TenantBO selectById(Long id) {
        TenantDO entityDO = getDOById(id, true);
        return tenantBuilder.buildBOByDO(entityDO);
    }

    @Override
    public TenantBO selectByCode(String code) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getTenantCode, code);
        wrapper.eq(TenantDO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantDO entityDO = tenantManager.getOne(wrapper);
        return tenantBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<TenantBO> selectByPage(TenantQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<TenantDO> entityPageDO = tenantManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return tenantBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<TenantDO> fuzzyQuery(TenantQuery entityQuery) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getTenantName()), TenantDO::getTenantName, entityQuery.getTenantName());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link TenantBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(TenantBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getTenantName, entityBO.getTenantName());
        wrapper.eq(TenantDO::getTenantCode, entityBO.getTenantCode());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        TenantDO one = tenantManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("租户重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link TenantDO}
     */
    private TenantDO getDOById(Long id, boolean throwException) {
        TenantDO entityDO = tenantManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("租户");
        }
        return entityDO;
    }
}
