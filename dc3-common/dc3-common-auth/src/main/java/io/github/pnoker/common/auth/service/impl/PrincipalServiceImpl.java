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
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.builder.PrincipalBuilder;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.query.PrincipalQuery;
import io.github.pnoker.common.auth.service.PrincipalService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Principal service implementation.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalBuilder principalBuilder;

    private final PrincipalManager principalManager;

    @Override
    public PrincipalBO getById(Long id) {
        return principalBuilder.buildBOByDO(getDOById(id, true));
    }

    @Override
    public Page<PrincipalBO> list(PrincipalQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PrincipalDO> page = principalManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return principalBuilder.buildBOPageByDOPage(page);
    }

    @Override
    public void setEnableFlag(Long id, EnableFlagEnum target, Long operatorId, String operatorName) {
        PrincipalDO current = getDOById(id, true);
        current.setEnableFlag(target.getIndex());
        current.setOperatorId(operatorId);
        current.setOperatorName(operatorName);
        current.setOperateTime(null);
        principalManager.updateById(current);
    }

    private LambdaQueryWrapper<PrincipalDO> fuzzyQuery(PrincipalQuery entityQuery) {
        LambdaQueryWrapper<PrincipalDO> wrapper = Wrappers.<PrincipalDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalType()), PrincipalDO::getPrincipalType,
                Objects.isNull(entityQuery.getPrincipalType()) ? null : entityQuery.getPrincipalType().getValue());
        wrapper.like(StringUtils.isNotBlank(entityQuery.getPrincipalName()), PrincipalDO::getPrincipalName,
                entityQuery.getPrincipalName());
        wrapper.like(StringUtils.isNotBlank(entityQuery.getDisplayName()), PrincipalDO::getDisplayName,
                entityQuery.getDisplayName());
        wrapper.eq(Objects.nonNull(entityQuery.getSourceType()), PrincipalDO::getSourceType,
                Objects.isNull(entityQuery.getSourceType()) ? null : entityQuery.getSourceType().getValue());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), PrincipalDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    private PrincipalDO getDOById(Long id, boolean throwException) {
        PrincipalDO entityDO = principalManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Principal does not exist");
        }
        return entityDO;
    }
}
