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
import com.pnoker.center.dbs.mapper.RtmpMapper;
import com.pnoker.center.dbs.service.RtmpService;
import com.pnoker.common.constant.Common;
import com.pnoker.common.bean.PageInfo;
import com.pnoker.common.entity.rtmp.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>Rtmp 接口实现
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
@Transactional
public class RtmpServiceImpl implements RtmpService {
    @Resource
    private RtmpMapper rtmpMapper;

    @Override
    @Caching(
            put = {@CachePut(value = "dbs_rtmp", key = "#rtmp.id", unless = "#result==null")},
            evict = {@CacheEvict(value = "dbs_rtmp_list", allEntries = true)}
    )
    public Rtmp add(Rtmp rtmp) {
        return rtmpMapper.insert(rtmp) > 0 ? rtmp : null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "dbs_rtmp", key = "#id"),
                    @CacheEvict(value = "dbs_rtmp_list", allEntries = true)
            }
    )
    public boolean delete(Long id) {
        return rtmpMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {@CachePut(value = "dbs_rtmp", key = "#rtmp.id", unless = "#result==null")},
            evict = {@CacheEvict(value = "dbs_rtmp_list", allEntries = true)}
    )
    public Rtmp update(Rtmp rtmp) {
        return rtmpMapper.updateById(rtmp) > 0 ? rtmp : null;
    }

    @Override
    @Cacheable(value = "dbs_rtmp", key = "#id", unless = "#result==null")
    public Rtmp selectById(Long id) {
        return rtmpMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "dbs_rtmp_list", keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Rtmp> list(Rtmp rtmp, PageInfo pageInfo) {
        return rtmpMapper.selectPage(pagination(pageInfo), fuzzyQuery(rtmp));
    }

    @Override
    public QueryWrapper<Rtmp> fuzzyQuery(Rtmp rtmp) {
        QueryWrapper<Rtmp> queryWrapper = new QueryWrapper<>();
        if (null != rtmp.getAutoStart()) {
            queryWrapper.eq(Common.Cloumn.Rtmp.AUTO_START, BooleanUtils.isTrue(rtmp.getAutoStart()));
        }
        if (StringUtils.isNotBlank(rtmp.getName())) {
            queryWrapper.like(Common.Cloumn.NAME, rtmp.getName());
        }
        return queryWrapper;
    }

    @Override
    public Page<Rtmp> pagination(PageInfo pageInfo) {
        Page<Rtmp> page = new Page<>(pageInfo.getPageNum(), pageInfo.getPageSize());
        Optional.ofNullable(pageInfo.getOrders()).ifPresent(orderItems -> {
            List<OrderItem> tmps = new ArrayList<>();
            orderItems.forEach(orderItem -> {
                if (Common.Cloumn.ID.equals(orderItem.getColumn())) {
                    tmps.add(orderItem);
                }
                if (Common.Cloumn.NAME.equals(orderItem.getColumn())) {
                    tmps.add(orderItem);
                }
            });
            page.setOrders(tmps);
        });
        return page;
    }

}
