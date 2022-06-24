/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.LabelBindMapper;
import com.dc3.center.manager.service.LabelBindService;
import com.dc3.common.bean.Pages;
import com.dc3.common.dto.LabelBindDto;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.LabelBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * LabelBindService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class LabelBindServiceImpl implements LabelBindService {
    @Resource
    private LabelBindMapper labelBindMapper;

    @Override
    public LabelBind add(LabelBind labelBind) {
        if (labelBindMapper.insert(labelBind) > 0) {
            return labelBindMapper.selectById(labelBind.getId());
        }
        throw new ServiceException("The label bind add failed");
    }

    @Override
    public boolean delete(String id) {
        selectById(id);
        return labelBindMapper.deleteById(id) > 0;
    }

    @Override
    public LabelBind update(LabelBind labelBind) {
        selectById(labelBind.getId());
        labelBind.setUpdateTime(null);
        if (labelBindMapper.updateById(labelBind) > 0) {
            return labelBindMapper.selectById(labelBind.getId());
        }
        throw new ServiceException("The label bind update failed");
    }

    @Override
    public LabelBind selectById(String id) {
        LabelBind labelBind = labelBindMapper.selectById(id);
        if (null == labelBind) {
            throw new NotFoundException("The label bind does not exist");
        }
        return labelBind;
    }

    @Override
    public Page<LabelBind> list(LabelBindDto labelBindDto) {
        if (!Optional.ofNullable(labelBindDto.getPage()).isPresent()) {
            labelBindDto.setPage(new Pages());
        }
        return labelBindMapper.selectPage(labelBindDto.getPage().convert(), fuzzyQuery(labelBindDto));
    }

    @Override
    public LambdaQueryWrapper<LabelBind> fuzzyQuery(LabelBindDto labelBindDto) {
        LambdaQueryWrapper<LabelBind> queryWrapper = Wrappers.<LabelBind>query().lambda();
        if (null != labelBindDto) {
            if (null != labelBindDto.getLabelId()) {
                queryWrapper.eq(LabelBind::getLabelId, labelBindDto.getLabelId());
            }
            if (null != labelBindDto.getEntityId()) {
                queryWrapper.eq(LabelBind::getEntityId, labelBindDto.getEntityId());
            }
            if (StrUtil.isNotBlank(labelBindDto.getType())) {
                queryWrapper.eq(LabelBind::getType, labelBindDto.getType());
            }
        }
        return queryWrapper;
    }

}
