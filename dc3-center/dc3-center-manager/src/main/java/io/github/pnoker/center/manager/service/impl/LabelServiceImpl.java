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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.model.LabelBindDO;
import io.github.pnoker.center.manager.entity.model.LabelDO;
import io.github.pnoker.center.manager.entity.query.LabelPageQuery;
import io.github.pnoker.center.manager.manager.LabelBindManager;
import io.github.pnoker.center.manager.manager.LabelManager;
import io.github.pnoker.center.manager.service.LabelService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
    private LabelManager labelManager;

    @Resource
    private LabelBindManager labelBindManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(LabelDO entityDO) {
        selectByName(entityDO.getLabelName(), entityDO.getTenantId());
        if (!labelManager.save(entityDO)) {
            throw new AddException("The label {} add failed", entityDO.getLabelName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        LambdaQueryWrapper<LabelBindDO> queryWrapper = Wrappers.<LabelBindDO>query().lambda();
        queryWrapper.eq(LabelBindDO::getLabelId, id);
        List<LabelBindDO> labelBindPage = labelBindManager.list(queryWrapper);
        if (CollUtil.isNotEmpty(labelBindPage)) {
            throw new ServiceException("The label already bound by the entity");
        }
        LabelDO label = selectById(id);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException("The label does not exist");
        }

        if (!labelManager.removeById(id)) {
            throw new DeleteException("The label delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(LabelDO entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (!labelManager.updateById(entityDO)) {
            throw new UpdateException("The label update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelDO selectById(Long id) {
        LabelDO label = labelManager.getById(id);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelDO selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<LabelDO> queryWrapper = Wrappers.<LabelDO>query().lambda();
        queryWrapper.eq(LabelDO::getLabelName, name);
        queryWrapper.eq(LabelDO::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        LabelDO label = labelManager.getOne(queryWrapper);
        if (ObjectUtil.isNull(label)) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<LabelDO> list(LabelPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return labelManager.page(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<LabelDO> fuzzyQuery(LabelPageQuery query) {
        LambdaQueryWrapper<LabelDO> queryWrapper = Wrappers.<LabelDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getLabelName()), LabelDO::getLabelName, query.getLabelName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getColor()), LabelDO::getColor, query.getColor());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getTenantId()), LabelDO::getTenantId, query.getTenantId());
        }
        return queryWrapper;
    }

}
