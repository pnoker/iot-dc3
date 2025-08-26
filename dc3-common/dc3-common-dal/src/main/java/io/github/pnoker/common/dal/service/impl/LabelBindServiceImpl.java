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

package io.github.pnoker.common.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.dal.dal.LabelBindManager;
import io.github.pnoker.common.dal.entity.bo.LabelBindBO;
import io.github.pnoker.common.dal.entity.builder.LabelBindBuilder;
import io.github.pnoker.common.dal.entity.model.LabelBindDO;
import io.github.pnoker.common.dal.entity.query.LabelBindQuery;
import io.github.pnoker.common.dal.service.LabelBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * LabelBindService Impl
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class LabelBindServiceImpl implements LabelBindService {

    @Resource
    private LabelBindBuilder labelBindBuilder;

    @Resource
    private LabelBindManager labelBindManager;

    @Override
    public void save(LabelBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        LabelBindDO entityDO = labelBindBuilder.buildDOByBO(entityBO);
        if (!labelBindManager.save(entityDO)) {
            throw new AddException("Failed to create label bind");
        }
    }


    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!labelBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove label bind");
        }
    }


    @Override
    public void update(LabelBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        LabelBindDO entityDO = labelBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!labelBindManager.updateById(entityDO)) {
            throw new UpdateException("The label bind update failed");
        }
    }


    @Override
    public LabelBindBO selectById(Long id) {
        LabelBindDO entityDO = getDOById(id, false);
        return labelBindBuilder.buildBOByDO(entityDO);
    }


    @Override
    public Page<LabelBindBO> selectByPage(LabelBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LabelBindDO> entityPageDO = labelBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return labelBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link LabelBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<LabelBindDO> fuzzyQuery(LabelBindQuery entityQuery) {
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getLabelId()), LabelBindDO::getLabelId, entityQuery.getLabelId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEntityId()), LabelBindDO::getEntityId, entityQuery.getEntityId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link LabelBindBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(LabelBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(LabelBindDO::getEntityTypeFlag, entityBO.getEntityTypeFlag());
        wrapper.eq(LabelBindDO::getLabelId, entityBO.getLabelId());
        wrapper.eq(LabelBindDO::getEntityId, entityBO.getEntityId());
        wrapper.eq(LabelBindDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LabelBindDO one = labelBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Label bind has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link LabelBindDO}
     */
    private LabelBindDO getDOById(Long id, boolean throwException) {
        LabelBindDO entityDO = labelBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Label bind does not exist");
        }
        return entityDO;
    }

}
