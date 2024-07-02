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

package io.github.pnoker.common.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.ApiManager;
import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.builder.ApiBuilder;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.query.ApiQuery;
import io.github.pnoker.common.auth.service.ApiService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Api Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ApiServiceImpl implements ApiService {

    @Resource
    private ApiBuilder apiBuilder;

    @Resource
    private ApiManager apiManager;

    @Override
    public void save(ApiBO entityBO) {
        checkDuplicate(entityBO, false, true);

        ApiDO entityDO = apiBuilder.buildDOByBO(entityBO);
        if (!apiManager.save(entityDO)) {
            throw new AddException("Failed to create api");
        }
    }


    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!apiManager.removeById(id)) {
            throw new DeleteException("Failed to remove api");
        }
    }


    @Override
    public void update(ApiBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        ApiDO entityDO = apiBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!apiManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update api");
        }
    }


    @Override
    public ApiBO selectById(Long id) {
        ApiDO entityDO = getDOById(id, true);
        return apiBuilder.buildBOByDO(entityDO);
    }


    @Override
    public Page<ApiBO> selectByPage(ApiQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ApiDO> entityPageDO = apiManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return apiBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link ApiQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<ApiDO> fuzzyQuery(ApiQuery entityQuery) {
        LambdaQueryWrapper<ApiDO> wrapper = Wrappers.<ApiDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getApiName()), ApiDO::getApiName, entityQuery.getApiName());
        wrapper.eq(ApiDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link ApiBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(ApiBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ApiDO> wrapper = Wrappers.<ApiDO>query().lambda();
        wrapper.eq(ApiDO::getApiTypeFlag, entityBO.getApiTypeFlag());
        wrapper.eq(ApiDO::getApiName, entityBO.getApiName());
        wrapper.eq(ApiDO::getApiCode, entityBO.getApiCode());
        wrapper.eq(ApiDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ApiDO one = apiManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Api has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link ApiDO}
     */
    private ApiDO getDOById(Long id, boolean throwException) {
        ApiDO entityDO = apiManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Api does not exist");
        }
        return entityDO;
    }

}
