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

package com.pnoker.center.dbs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.dbs.mapper.TokenMapper;
import com.pnoker.center.dbs.mapper.UserMapper;
import com.pnoker.center.dbs.service.UserService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>User 接口实现
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private TokenMapper tokenMapper;

    @Override
    @Caching(
            put = {@CachePut(value = "dbs_user", key = "#user.id", unless = "#result==null")},
            evict = {@CacheEvict(value = "dbs_user_list", allEntries = true)}
    )
    public User add(User user) {
        return userMapper.insert(user) > 0 ? user : null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "dbs_user", key = "#id"),
                    @CacheEvict(value = "dbs_user_token_id", allEntries = true),
                    @CacheEvict(value = "dbs_user_token_app_id", allEntries = true),
                    @CacheEvict(value = "dbs_user_list", allEntries = true)
            }
    )
    public boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {@CachePut(value = "dbs_user", key = "#user.id", unless = "#result==null")},
            evict = {
                    @CacheEvict(value = "dbs_user_token_id", allEntries = true),
                    @CacheEvict(value = "dbs_user_token_app_id", allEntries = true),
                    @CacheEvict(value = "dbs_user_list", allEntries = true)
            }
    )
    public User update(User user) {
        return userMapper.updateById(user) > 0 ? user : null;
    }

    @Override
    @Cacheable(value = "dbs_user", key = "#id", unless = "#result==null")
    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "dbs_user_list", keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<User> list(UserDto userDto) {
        return userMapper.selectPage(pagination(userDto.getPage()), fuzzyQuery(userDto));
    }

    @Override
    public QueryWrapper<User> fuzzyQuery(UserDto userDto) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Optional.ofNullable(userDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getUsername())) {
                queryWrapper.like(Common.Cloumn.User.USERNAME, dto.getUsername());
            }
            if (StringUtils.isNotBlank(dto.getPhone())) {
                queryWrapper.like(Common.Cloumn.User.PHONE, dto.getPhone());
            }
            if (StringUtils.isNotBlank(dto.getEmail())) {
                queryWrapper.like(Common.Cloumn.User.EMAIL, dto.getEmail());
            }
        });
        return queryWrapper;
    }

    @Override
    public Page<User> pagination(Pages pages) {
        Page<User> page = new Page<>(pages.getPageNum(), pages.getPageSize());
        Optional.ofNullable(pages.getOrders()).ifPresent(orderItems -> {
            List<OrderItem> tmps = new ArrayList<>();
            orderItems.forEach(item -> {
                if (Common.Cloumn.ID.equals(item.getColumn())) {
                    tmps.add(item);
                }
                if (Common.Cloumn.User.USERNAME.equals(item.getColumn())) {
                    tmps.add(item);
                }
            });
            page.setOrders(tmps);
        });
        return page;
    }

    @Override
    @Cacheable(value = "dbs_user", key = "#usernama", unless = "#result==null")
    public User selectByUsername(String usernama) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Common.Cloumn.User.USERNAME, usernama);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = "dbs_user", key = "#phone", unless = "#result==null")
    public User selectByPhone(String phone) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Common.Cloumn.User.PHONE, phone);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = "dbs_user", key = "#email", unless = "#result==null")
    public User selectByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Common.Cloumn.User.EMAIL, email);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "dbs_user_token_id", key = "#token.id", unless = "#result==null"),
                    @CachePut(value = "dbs_user_token_app_id", key = "#token.appId", unless = "#result==null")
            }
    )
    public Token updateToken(Token token) {
        return tokenMapper.updateById(token) > 0 ? token : null;
    }

    @Override
    @Cacheable(value = "dbs_user_token_id", key = "#id", unless = "#result==null")
    public Token selectTokenById(Long id) {
        return tokenMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "dbs_user_token_app_id", key = "#appId", unless = "#result==null")
    public Token selectTokenByAppId(String appId) {
        QueryWrapper<Token> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Common.Cloumn.Token.APP_ID, appId);
        return tokenMapper.selectOne(queryWrapper);
    }

}
