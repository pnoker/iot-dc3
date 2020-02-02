/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.device.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.LabelBindDto;
import com.pnoker.common.dto.LabelDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Label;
import com.pnoker.common.model.LabelBind;
import com.pnoker.device.manager.mapper.LabelMapper;
import com.pnoker.device.manager.service.LabelBindService;
import com.pnoker.device.manager.service.LabelService;
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
 * <p>标签服务接口实现类
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
                    @CachePut(value = Common.Cache.LABEL_ID, key = "#label.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.LABEL_NAME, key = "#label.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.LABEL_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Label add(Label label) {
        Label select = selectByName(label.getName());
        if (null != select) {
            throw new ServiceException("label already exists");
        }
        if (labelMapper.insert(label) > 0) {
            return labelMapper.selectById(label.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        LabelBindDto labelBindDto = new LabelBindDto();
        labelBindDto.setLabelId(id);
        Page<LabelBind> labelBindPage = labelBindService.list(labelBindDto);
        if (labelBindPage.getTotal() > 0) {
            throw new ServiceException("label already bound by the entity");
        }
        return labelMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.LABEL_ID, key = "#label.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.LABEL_NAME, key = "#label.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.LABEL_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Label update(Label label) {
        if (labelMapper.updateById(label) > 0) {
            Label select = selectById(label.getId());
            label.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL_ID, key = "#id", unless = "#result==null")
    public Label selectById(Long id) {
        return labelMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL_NAME, key = "#name", unless = "#result==null")
    public Label selectByName(String name) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        queryWrapper.like(Label::getName, name);
        return labelMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Label> list(LabelDto labelDto) {
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
