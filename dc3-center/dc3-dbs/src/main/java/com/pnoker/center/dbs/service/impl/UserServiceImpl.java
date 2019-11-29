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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.dbs.mapper.UserMapper;
import com.pnoker.center.dbs.service.UserService;
import com.pnoker.common.base.PageInfo;
import com.pnoker.common.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
    @Autowired
    private UserMapper userMapper;

    @Override
    @CachePut(value = "user", key = "#user.id")
    public boolean add(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    @CacheEvict(value = "user", key = "#user.id")
    public boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    @CachePut(value = "user", key = "#user.id")
    public boolean update(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Cacheable(value = "user", key = "#a0", unless = "#result == null")
    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "user", key = "#a0", unless = "#result == null")
    public User selectByUsername(String usernama) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", usernama);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<User> list(User user, PageInfo pageInfo) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        query(user, queryWrapper);
        Page<User> page = new Page<>(pageInfo.getPageNum(), pageInfo.getPageSize());
        return userMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void query(User user, QueryWrapper<User> queryWrapper) {
        Optional.ofNullable(user).ifPresent(u -> {
            if (StringUtils.isNotBlank(u.getUsername())) {
                queryWrapper.like("username", u.getUsername());
            }
            if (StringUtils.isNotBlank(u.getPhone())) {
                queryWrapper.like("phone", u.getPhone());
            }
            if (StringUtils.isNotBlank(u.getEmail())) {
                queryWrapper.like("email", u.getEmail());
            }
        });
    }

    @Override
    public void order(List<OrderItem> orders, Page<User> page) {
        Optional.ofNullable(orders).ifPresent(orderItems -> {
            List<OrderItem> tmps = new ArrayList<>();
            orderItems.stream().forEach(orderItem -> {
                if ("id".equals(orderItem.getColumn())) {
                    orderItem.setAsc(BooleanUtils.isTrue(orderItem.isAsc()));
                    tmps.add(orderItem);
                }
                if ("username".equals(orderItem.getColumn())) {
                    orderItem.setAsc(BooleanUtils.isTrue(orderItem.isAsc()));
                    tmps.add(orderItem);
                }
            });
            page.setOrders(tmps);
        });
    }

}
