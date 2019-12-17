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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.dbs.mapper.TokenMapper;
import com.pnoker.center.dbs.service.TokenDbsService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>TokenDbsServiceImpl
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class TokenDbsServiceImpl implements TokenDbsService {
    @Resource
    private TokenMapper tokenMapper;

    @Override
    @CachePut(value = "dbs_token", key = "#token.id", unless = "#result==null")
    public Token add(Token token) {
        if (tokenMapper.insert(token) > 0) {
            token = tokenMapper.selectById(token.getId());
            return token;
        }
        return null;
    }

    @Override
    @CacheEvict(value = "dbs_token", key = "#id")
    public boolean delete(Long id) {
        return tokenMapper.deleteById(id) > 0;
    }

    @Override
    @CachePut(value = "dbs_token", key = "#token.id", unless = "#result==null")
    public Token update(Token token) {
        if (tokenMapper.updateById(token) > 0) {
            token = tokenMapper.selectById(token.getId());
            return token;
        }
        return null;
    }

    @Override
    @Cacheable(value = "dbs_token", key = "#id", unless = "#result==null")
    public Token selectById(Long id) {
        return tokenMapper.selectById(id);
    }

    /**
     * Token不提供List查询
     */
    @Override
    public Page<Token> list(TokenDto tokenDto) {
        return null;
    }

    @Override
    public QueryWrapper<Token> fuzzyQuery(TokenDto tokenDto) {
        return null;
    }

    @Override
    public Page<Token> pagination(Pages pages) {
        return null;
    }

}
