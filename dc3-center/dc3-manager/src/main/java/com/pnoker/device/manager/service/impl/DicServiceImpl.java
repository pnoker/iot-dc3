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
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DicDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Dic;
import com.pnoker.device.manager.mapper.DicMapper;
import com.pnoker.device.manager.service.DicService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * <p>字典服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DicServiceImpl implements DicService {

    @Resource
    private DicMapper dicMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DIC_ID, key = "#dic.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DIC_LABEL_TYPE, key = "#dic.label+'.'+#dic.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DIC_DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DIC_LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Dic add(Dic dic) {
        Dic select = selectByLabelAndType(dic.getLabel(), dic.getType());
        if (null != select) {
            throw new ServiceException("dic already exists");
        }
        if (dicMapper.insert(dic) > 0) {
            return dicMapper.selectById(dic.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DIC_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DIC_LABEL_TYPE, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DIC_DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DIC_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return dicMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DIC_ID, key = "#dic.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DIC_LABEL_TYPE, key = "#dic.label+'.'+#dic.type", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DIC_DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DIC_LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Dic update(Dic dic) {
        dic.setUpdateTime(null);
        if (dicMapper.updateById(dic) > 0) {
            Dic select = selectById(dic.getId());
            dic.setLabel(select.getLabel());
            dic.setType(select.getType());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.DIC_ID, key = "#id", unless = "#result==null")
    public Dic selectById(Long id) {
        return dicMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DIC_LABEL_TYPE, key = "#label+'.'+#type", unless = "#result==null")
    public Dic selectByLabelAndType(String label, String type) {
        LambdaQueryWrapper<Dic> queryWrapper = Wrappers.<Dic>query().lambda();
        queryWrapper.like(Dic::getLabel, label);
        return dicMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.DIC_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Dic> list(DicDto dicDto) {
        if (!Optional.ofNullable(dicDto.getPage()).isPresent()) {
            dicDto.setPage(new Pages());
        }
        return dicMapper.selectPage(dicDto.getPage().convert(), fuzzyQuery(dicDto));
    }

    @Override
    @Cacheable(value = Common.Cache.DIC_DIC, key = "'dic_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        LambdaQueryWrapper<Dic> queryWrapper = Wrappers.<Dic>query().lambda();
        return dicMapper.selectList(queryWrapper);
    }

    @Override
    public LambdaQueryWrapper<Dic> fuzzyQuery(DicDto dicDto) {
        LambdaQueryWrapper<Dic> queryWrapper = Wrappers.<Dic>query().lambda();
        Optional.ofNullable(dicDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getLabel())) {
                queryWrapper.like(Dic::getLabel, dto.getLabel());
            }
            if (StringUtils.isNotBlank(dto.getType())) {
                queryWrapper.like(Dic::getType, dto.getType());
            }
        });
        return queryWrapper;
    }

}
