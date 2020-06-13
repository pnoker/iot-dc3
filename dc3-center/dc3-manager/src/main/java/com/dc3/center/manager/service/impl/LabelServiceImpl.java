/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.LabelMapper;
import com.dc3.center.manager.service.LabelBindService;
import com.dc3.center.manager.service.LabelService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.LabelBindDto;
import com.dc3.common.dto.LabelDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Label;
import com.dc3.common.model.LabelBind;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>LabelService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class LabelServiceImpl implements LabelService {
    @Resource
    private LabelBindService labelBindService;
    @Resource
    private LabelMapper labelMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.LABEL + Common.Cache.ID, key = "#label.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.LABEL + Common.Cache.NAME, key = "#label.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Label add(Label label) {
        Label select = selectByName(label.getName());
        Optional.ofNullable(select).ifPresent(l -> {
            throw new ServiceException("The label already exists");
        });
        if (labelMapper.insert(label) > 0) {
            return labelMapper.selectById(label.getId());
        }
        throw new ServiceException("The label add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        LabelBindDto labelBindDto = new LabelBindDto();
        labelBindDto.setLabelId(id);
        Page<LabelBind> labelBindPage = labelBindService.list(labelBindDto);
        if (labelBindPage.getTotal() > 0) {
            throw new ServiceException("The label already bound by the entity");
        }
        Label label = selectById(id);
        if (null == label) {
            throw new ServiceException("The label does not exist");
        }
        return labelMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.LABEL + Common.Cache.ID, key = "#label.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.LABEL + Common.Cache.NAME, key = "#label.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.LABEL + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Label update(Label label) {
        Label temp = selectById(label.getId());
        if (null == temp) {
            throw new ServiceException("The label does not exist");
        }
        label.setUpdateTime(null);
        if (labelMapper.updateById(label) > 0) {
            Label select = labelMapper.selectById(label.getId());
            label.setName(select.getName());
            return select;
        }
        throw new ServiceException("The label update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Label selectById(Long id) {
        return labelMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public Label selectByName(String name) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        queryWrapper.eq(Label::getName, name);
        return labelMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Label> list(LabelDto labelDto) {
        if (!Optional.ofNullable(labelDto.getPage()).isPresent()) {
            labelDto.setPage(new Pages());
        }
        return labelMapper.selectPage(labelDto.getPage().convert(), fuzzyQuery(labelDto));
    }

    @Override
    public LambdaQueryWrapper<Label> fuzzyQuery(LabelDto labelDto) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        Optional.ofNullable(labelDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Label::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getColor())) {
                queryWrapper.eq(Label::getColor, dto.getColor());
            }
        });
        return queryWrapper;
    }

}
