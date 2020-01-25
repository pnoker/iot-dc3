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
import com.pnoker.common.dto.ConnectInfoDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.ConnectInfo;
import com.pnoker.device.manager.mapper.ConnectInfoMapper;
import com.pnoker.device.manager.service.ConnectInfoService;
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
 * <p>连接信息服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ConnectInfoServiceImpl implements ConnectInfoService {

    @Resource
    private ConnectInfoMapper connectInfoMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.CONNECT_INFO_ID, key = "#connectInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.CONNECT_INFO_NAME, key = "#connectInfo.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.CONNECT_INFO_LIST, allEntries = true, condition = "#result!=null")}
    )
    public ConnectInfo add(ConnectInfo connectInfo) {
        ConnectInfo select = selectByName(connectInfo.getName());
        if (null != select) {
            throw new ServiceException("connect info already exists");
        }
        if (connectInfoMapper.insert(connectInfo) > 0) {
            return connectInfoMapper.selectById(connectInfo.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.CONNECT_INFO_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.CONNECT_INFO_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.CONNECT_INFO_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return connectInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.CONNECT_INFO_ID, key = "#connectInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.CONNECT_INFO_NAME, key = "#connectInfo.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.CONNECT_INFO_LIST, allEntries = true, condition = "#result==true")}
    )
    public ConnectInfo update(ConnectInfo connectInfo) {
        if (connectInfoMapper.updateById(connectInfo) > 0) {
            ConnectInfo select = selectById(connectInfo.getId());
            connectInfo.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.CONNECT_INFO_ID, key = "#id", unless = "#result==null")
    public ConnectInfo selectById(Long id) {
        return connectInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.CONNECT_INFO_NAME, key = "#name", unless = "#result==null")
    public ConnectInfo selectByName(String name) {
        LambdaQueryWrapper<ConnectInfo> queryWrapper = Wrappers.<ConnectInfo>query().lambda();
        queryWrapper.like(ConnectInfo::getName, name);
        return connectInfoMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.CONNECT_INFO_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<ConnectInfo> list(ConnectInfoDto connectInfoDto) {
        return connectInfoMapper.selectPage(connectInfoDto.getPage().convert(), fuzzyQuery(connectInfoDto));
    }

    @Override
    public LambdaQueryWrapper<ConnectInfo> fuzzyQuery(ConnectInfoDto connectInfoDto) {
        LambdaQueryWrapper<ConnectInfo> queryWrapper = Wrappers.<ConnectInfo>query().lambda();
        Optional.ofNullable(connectInfoDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(ConnectInfo::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
