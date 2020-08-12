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

package com.dc3.center.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.auth.mapper.UserMapper;
import com.dc3.center.auth.service.UserService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.UserDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.User;
import com.dc3.common.utils.Dc3Util;
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
 * 用户服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.USER + Common.Cache.ID, key = "#user.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.USER + Common.Cache.NAME, key = "#user.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public User add(User user) {
        User select = selectByName(user.getName());
        if (null != select) {
            throw new ServiceException("The user already exists");
        }
        if (userMapper.insert(user.setPassword(Dc3Util.md5(user.getPassword()))) > 0) {
            return userMapper.selectById(user.getId());
        }
        throw new ServiceException("The user add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        User user = selectById(id);
        if (null == user) {
            throw new ServiceException("The user does not exist");
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.USER + Common.Cache.ID, key = "#user.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.USER + Common.Cache.NAME, key = "#user.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.USER + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public User update(User user) {
        user.setName(null).setUpdateTime(null);
        if (userMapper.updateById(user) > 0) {
            User select = userMapper.selectById(user.getId());
            user.setName(select.getName());
            return select;
        }
        throw new ServiceException("The user update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.USER + Common.Cache.ID, key = "#id", unless = "#result==null")
    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.USER + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public User selectByName(String name) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda()
                .eq(User::getName, name);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.USER + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<User> list(UserDto userDto) {
        if (!Optional.ofNullable(userDto.getPage()).isPresent()) {
            userDto.setPage(new Pages());
        }
        return userMapper.selectPage(userDto.getPage().convert(), fuzzyQuery(userDto));
    }

    @Override
    public boolean checkUserValid(String name) {
        User user = selectByName(name);
        if (null != user) {
            return user.getEnable();
        }
        return false;
    }

    @Override
    public boolean restPassword(Long id) {
        User user = selectById(id);
        if (null != user) {
            user.setPassword(Dc3Util.md5(Common.DEFAULT_PASSWORD));
            return null != update(user);
        }
        return false;
    }

    @Override
    public LambdaQueryWrapper<User> fuzzyQuery(UserDto userDto) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        Optional.ofNullable(userDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(User::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
