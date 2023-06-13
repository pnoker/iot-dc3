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
import io.github.pnoker.center.manager.mapper.LabelBindMapper;
import io.github.pnoker.center.manager.service.LabelBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.model.LabelBind;
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
    private LabelBindMapper labelBindMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(LabelBind entityDO) {
        if (labelBindMapper.insert(entityDO) < 1) {
            throw new AddException("The label bind add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        LabelBind labelBind = selectById(id);
        if (ObjectUtil.isNull(labelBind)) {
            throw new NotFoundException("The label bind does not exist");
        }

        if (labelBindMapper.deleteById(id) < 1) {
            throw new DeleteException("The label bind delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(LabelBind entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (labelBindMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The label bind update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelBind selectById(String id) {
        LabelBind labelBind = labelBindMapper.selectById(id);
        if (ObjectUtil.isNull(labelBind)) {
            throw new NotFoundException();
        }
        return labelBind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<LabelBind> list(LabelBindPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return labelBindMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<LabelBind> fuzzyQuery(LabelBindPageQuery query) {
        LambdaQueryWrapper<LabelBind> queryWrapper = Wrappers.<LabelBind>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getLabelId()), LabelBind::getLabelId, query.getLabelId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getEntityId()), LabelBind::getEntityId, query.getEntityId());
        }
        return queryWrapper;
    }

}
