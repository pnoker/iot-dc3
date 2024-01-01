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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.dal.LabelBindManager;
import io.github.pnoker.center.manager.entity.builder.LabelBindForManagerBuilder;
import io.github.pnoker.center.manager.entity.model.LabelBindDO;
import io.github.pnoker.center.manager.entity.query.LabelBindQuery;
import io.github.pnoker.center.manager.service.LabelBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.bo.LabelBindBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * LabelBindService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class LabelBindServiceImpl implements LabelBindService {

    @Resource
    private LabelBindForManagerBuilder labelBindForManagerBuilder;

    @Resource
    private LabelBindManager labelBindManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(LabelBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        LabelBindDO entityDO = labelBindForManagerBuilder.buildDOByBO(entityBO);
        if (!labelBindManager.save(entityDO)) {
            throw new AddException("The label bind add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!labelBindManager.removeById(id)) {
            throw new DeleteException("The label bind delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(LabelBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        LabelBindDO entityDO = labelBindForManagerBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!labelBindManager.updateById(entityDO)) {
            throw new UpdateException("The label bind update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelBindBO selectById(Long id) {
        LabelBindDO entityDO = getDOById(id, false);
        return labelBindForManagerBuilder.buildBOByDO(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<LabelBindBO> selectByPage(LabelBindQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LabelBindDO> entityPageDO = labelBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return labelBindForManagerBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param query {@link LabelBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<LabelBindDO> fuzzyQuery(LabelBindQuery query) {
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(CharSequenceUtil.isNotEmpty(query.getLabelId()), LabelBindDO::getLabelId, query.getLabelId());
        wrapper.eq(CharSequenceUtil.isNotEmpty(query.getEntityId()), LabelBindDO::getEntityId, query.getEntityId());
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
        wrapper.eq(LabelBindDO::getLabelId, entityBO.getLabelId());
        wrapper.eq(LabelBindDO::getEntityId, entityBO.getEntityId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LabelBindDO one = labelBindManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("The label bind is duplicates");
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
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("标签绑定不存在");
        }
        return entityDO;
    }

}
