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

package io.github.pnoker.common.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.bo.LabelBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.manager.dal.LabelBindManager;
import io.github.pnoker.common.manager.dal.LabelManager;
import io.github.pnoker.common.manager.entity.builder.LabelForManagerBuilder;
import io.github.pnoker.common.manager.entity.model.LabelBindDO;
import io.github.pnoker.common.manager.entity.model.LabelDO;
import io.github.pnoker.common.manager.entity.query.LabelQuery;
import io.github.pnoker.common.manager.service.LabelService;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * LabelService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class LabelServiceImpl implements LabelService {

    @Resource
    private LabelForManagerBuilder labelForManagerBuilder;

    @Resource
    private LabelManager labelManager;
    @Resource
    private LabelBindManager labelBindManager;

    @Override
    public void save(LabelBO entityBO) {
        checkDuplicate(entityBO, false, true);

        LabelDO entityDO = labelForManagerBuilder.buildDOByBO(entityBO);
        if (!labelManager.save(entityDO)) {
            throw new AddException("Failed to create label");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除标签之前需要检查该标签是否存在关联
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(LabelBindDO::getLabelId, id);
        long count = labelBindManager.count(wrapper);
        if (count > 0) {
            throw new AssociatedException("The label has been bound by another entity");
        }

        if (!labelManager.removeById(id)) {
            throw new DeleteException("Failed to remove label");
        }
    }

    @Override
    public void update(LabelBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        LabelDO entityDO = labelForManagerBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!labelManager.updateById(entityDO)) {
            throw new UpdateException("The label update failed");
        }
    }

    @Override
    public LabelBO selectById(Long id) {
        LabelDO entityDO = getDOById(id, false);
        return labelForManagerBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<LabelBO> selectByPage(LabelQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LabelDO> entityPageDO = labelManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return labelForManagerBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link LabelQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<LabelDO> fuzzyQuery(LabelQuery entityQuery) {
        LambdaQueryWrapper<LabelDO> wrapper = Wrappers.<LabelDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getLabelName()), LabelDO::getLabelName, entityQuery.getLabelName());
        wrapper.eq(Objects.nonNull(entityQuery.getEntityTypeFlag()), LabelDO::getEntityTypeFlag, entityQuery.getEntityTypeFlag());
        wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getColor()), LabelDO::getColor, entityQuery.getColor());
        wrapper.eq(LabelDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link LabelBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(LabelBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<LabelDO> wrapper = Wrappers.<LabelDO>query().lambda();
        wrapper.eq(LabelDO::getLabelName, entityBO.getLabelName());
        wrapper.eq(LabelDO::getEntityTypeFlag, entityBO.getEntityTypeFlag());
        wrapper.eq(LabelDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LabelDO one = labelManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Label has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link LabelDO}
     */
    private LabelDO getDOById(Long id, boolean throwException) {
        LabelDO entityDO = labelManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Label does not exist");
        }
        return entityDO;
    }
}
