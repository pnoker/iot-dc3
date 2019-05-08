/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.pnoker.dbs.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.rtmp.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@RestController
public class UserFeignClient extends BaseController implements UserFeignApi {

    @Override
    public String getById(@PathVariable("userId") Long userId) {
        log.info("search userId {}", userId);
        return "";
    }

    @Override
    public int insert(Rtmp entity) {
        return 0;
    }

    @Override
    public int deleteById(Serializable id) {
        return 0;
    }

    @Override
    public int deleteByMap(Map<String, Object> columnMap) {
        return 0;
    }

    @Override
    public int delete(Wrapper<Rtmp> wrapper) {
        return 0;
    }

    @Override
    public int deleteBatchIds(Collection<? extends Serializable> idList) {
        return 0;
    }

    @Override
    public int updateById(Rtmp entity) {
        return 0;
    }

    @Override
    public int update(Rtmp entity, Wrapper<Rtmp> updateWrapper) {
        return 0;
    }

    @Override
    public Rtmp selectById(Serializable id) {
        return null;
    }

    @Override
    public List<Rtmp> selectBatchIds(Collection<? extends Serializable> idList) {
        return null;
    }

    @Override
    public List<Rtmp> selectByMap(Map<String, Object> columnMap) {
        return null;
    }

    @Override
    public Rtmp selectOne(Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public Integer selectCount(Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public List<Rtmp> selectList(Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public List<Map<String, Object>> selectMaps(Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public List<Object> selectObjs(Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public IPage<Rtmp> selectPage(IPage<Rtmp> page, Wrapper<Rtmp> queryWrapper) {
        return null;
    }

    @Override
    public IPage<Map<String, Object>> selectMapsPage(IPage<Rtmp> page, Wrapper<Rtmp> queryWrapper) {
        return null;
    }
}
