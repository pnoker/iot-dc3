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
import io.github.pnoker.center.manager.entity.model.LabelBindDO;
import io.github.pnoker.center.manager.entity.query.LabelBindPageQuery;
import io.github.pnoker.center.manager.manager.LabelBindManager;
import io.github.pnoker.center.manager.service.LabelBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
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
    private LabelBindManager labelBindManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(LabelBindDO entityBO) {
        if (!labelBindManager.save(entityBO)) {
            throw new AddException("The label bind add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        LabelBindDO labelBind = get(id);
        if (ObjectUtil.isNull(labelBind)) {
            throw new NotFoundException("The label bind does not exist");
        }

        if (!labelBindManager.removeById(id)) {
            throw new DeleteException("The label bind delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(LabelBindDO entityBO) {
        get(entityBO.getId());
        entityBO.setOperateTime(null);
        if (!labelBindManager.updateById(entityBO)) {
            throw new UpdateException("The label bind update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelBindDO get(Long id) {
        LabelBindDO labelBind = labelBindManager.getById(id);
        if (ObjectUtil.isNull(labelBind)) {
            throw new NotFoundException();
        }
        return labelBind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<LabelBindDO> list(LabelBindPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return labelBindManager.page(entityQuery.getPage().page(), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<LabelBindDO> fuzzyQuery(LabelBindPageQuery query) {
        LambdaQueryWrapper<LabelBindDO> queryWrapper = Wrappers.<LabelBindDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getLabelId()), LabelBindDO::getLabelId, query.getLabelId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getEntityId()), LabelBindDO::getEntityId, query.getEntityId());
        }
        return queryWrapper;
    }

}
