/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.manager.entity.query.LabelBindPageQuery;
import io.github.pnoker.center.manager.entity.query.LabelPageQuery;
import io.github.pnoker.center.manager.mapper.LabelMapper;
import io.github.pnoker.center.manager.service.LabelBindService;
import io.github.pnoker.center.manager.service.LabelService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.Label;
import io.github.pnoker.common.model.LabelBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
    private LabelMapper labelMapper;

    @Resource
    private LabelBindService labelBindService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Label entityDO) {
        selectByName(entityDO.getLabelName(), entityDO.getTenantId());
        if (labelMapper.insert(entityDO) < 1) {
            throw new AddException("The label {} add failed", entityDO.getLabelName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        LabelBindPageQuery labelBindPageQuery = new LabelBindPageQuery();
        labelBindPageQuery.setLabelId(id);
        Page<LabelBind> labelBindPage = labelBindService.list(labelBindPageQuery);
        if (labelBindPage.getTotal() > 0) {
            throw new ServiceException("The label already bound by the entity");
        }
        Label label = selectById(id);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException("The label does not exist");
        }

        if (labelMapper.deleteById(id) < 1) {
            throw new DeleteException("The label delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Label entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (labelMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The label update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Label selectById(String id) {
        Label label = labelMapper.selectById(id);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Label selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        queryWrapper.eq(Label::getLabelName, name);
        queryWrapper.eq(Label::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Label label = labelMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Label> list(LabelPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return labelMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<Label> fuzzyQuery(LabelPageQuery query) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getLabelName()), Label::getLabelName, query.getLabelName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getColor()), Label::getColor, query.getColor());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), Label::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

}
