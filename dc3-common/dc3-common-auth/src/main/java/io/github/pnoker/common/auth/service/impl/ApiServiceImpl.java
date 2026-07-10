/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.service.impl;

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
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Business service implementation for API operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {

    private final ApiBuilder apiBuilder;

    private final ApiManager apiManager;

    @Override
    public void add(ApiBO entityBO) {
        checkDuplicate(entityBO, false, true);

        ApiDO entityDO = apiBuilder.buildDOByBO(entityBO);
        if (!apiManager.save(entityDO)) {
            throw new AddException("Failed to create api");
        }
    }

    @Override
    public void delete(Long id) {
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
    public ApiBO getById(Long id) {
        ApiDO entityDO = getDOById(id, true);
        return apiBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<ApiBO> list(ApiQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ApiDO> entityPageDO = apiManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return apiBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for api search.
     *
     * @param entityQuery {@link ApiQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link ApiDO}
     */
    private LambdaQueryWrapper<ApiDO> fuzzyQuery(ApiQuery entityQuery) {
        LambdaQueryWrapper<ApiDO> wrapper = Wrappers.<ApiDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getApiName()), ApiDO::getApiName, entityQuery.getApiName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getApiCode()), ApiDO::getApiCode, entityQuery.getApiCode());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getServiceName()), ApiDO::getServiceName,
                entityQuery.getServiceName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getApiGroup()), ApiDO::getApiGroup, entityQuery.getApiGroup());
        wrapper.eq(Objects.nonNull(entityQuery.getApiTypeFlag()), ApiDO::getApiTypeFlag,
                Objects.isNull(entityQuery.getApiTypeFlag()) ? null : entityQuery.getApiTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), ApiDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * Check whether an api is duplicated by api type, name, and code.
     *
     * @param entityBO       {@link ApiBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(ApiBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ApiDO> wrapper = Wrappers.<ApiDO>query().lambda();
        wrapper.eq(ApiDO::getApiTypeFlag, entityBO.getApiTypeFlag());
        wrapper.eq(ApiDO::getApiName, entityBO.getApiName());
        wrapper.eq(ApiDO::getApiCode, entityBO.getApiCode());
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
     * Get api data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link ApiDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private ApiDO getDOById(Long id, boolean throwException) {
        ApiDO entityDO = apiManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Api does not exist");
        }
        return entityDO;
    }

}
