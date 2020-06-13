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
import com.dc3.center.manager.mapper.LabelBindMapper;
import com.dc3.center.manager.service.LabelBindService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.LabelBindDto;
import com.dc3.common.exception.ServiceException;
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
 * <p>LabelBindService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class LabelBindServiceImpl implements LabelBindService {
    @Resource
    private LabelBindMapper labelBindMapper;

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.LABEL_BIND + Common.Cache.ID, key = "#labelBind.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public LabelBind add(LabelBind labelBind) {
        if (labelBindMapper.insert(labelBind) > 0) {
            return labelBindMapper.selectById(labelBind.getId());
        }
        throw new ServiceException("The label bind add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        LabelBind labelBind = selectById(id);
        if (null == labelBind) {
            throw new ServiceException("The label bind does not exist");
        }
        return labelBindMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.LABEL_BIND + Common.Cache.ID, key = "#labelBind.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.LABEL_BIND + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public LabelBind update(LabelBind labelBind) {
        LabelBind temp = selectById(labelBind.getId());
        if (null == temp) {
            throw new ServiceException("The label bind does not exist");
        }
        labelBind.setUpdateTime(null);
        if (labelBindMapper.updateById(labelBind) > 0) {
            return labelBindMapper.selectById(labelBind.getId());
        }
        throw new ServiceException("The label bind update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL_BIND + Common.Cache.ID, key = "#id", unless = "#result==null")
    public LabelBind selectById(Long id) {
        return labelBindMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.LABEL_BIND + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<LabelBind> list(LabelBindDto labelBindDto) {
        if (!Optional.ofNullable(labelBindDto.getPage()).isPresent()) {
            labelBindDto.setPage(new Pages());
        }
        return labelBindMapper.selectPage(labelBindDto.getPage().convert(), fuzzyQuery(labelBindDto));
    }

    @Override
    public LambdaQueryWrapper<LabelBind> fuzzyQuery(LabelBindDto labelBindDto) {
        LambdaQueryWrapper<LabelBind> queryWrapper = Wrappers.<LabelBind>query().lambda();
        Optional.ofNullable(labelBindDto).ifPresent(dto -> {
            if (null != dto.getLabelId()) {
                queryWrapper.eq(LabelBind::getLabelId, dto.getLabelId());
            }
            if (null != dto.getEntityId()) {
                queryWrapper.eq(LabelBind::getEntityId, dto.getEntityId());
            }
            if (StringUtils.isNotBlank(dto.getType())) {
                queryWrapper.eq(LabelBind::getType, dto.getType());
            }
        });
        return queryWrapper;
    }

}
