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

package com.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.manager.mapper.ScheduleMapper;
import com.pnoker.center.manager.service.ScheduleService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ScheduleDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Schedule;
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
 * <p>ScheduleService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Resource
    private ScheduleMapper scheduleMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.SCHEDULE + Common.Cache.ID, key = "#schedule.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.SCHEDULE + Common.Cache.NAME, key = "#schedule.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Schedule add(Schedule schedule) {
        Schedule select = selectByName(schedule.getName());
        if (null != select) {
            throw new ServiceException("device schedule already exists");
        }
        if (scheduleMapper.insert(schedule) > 0) {
            return scheduleMapper.selectById(schedule.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return scheduleMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.SCHEDULE + Common.Cache.ID, key = "#schedule.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.SCHEDULE + Common.Cache.NAME, key = "#schedule.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.SCHEDULE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Schedule update(Schedule schedule) {
        schedule.setUpdateTime(null);
        if (scheduleMapper.updateById(schedule) > 0) {
            Schedule select = selectById(schedule.getId());
            schedule.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.SCHEDULE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Schedule selectById(Long id) {
        return scheduleMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.SCHEDULE + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public Schedule selectByName(String name) {
        LambdaQueryWrapper<Schedule> queryWrapper = Wrappers.<Schedule>query().lambda();
        queryWrapper.like(Schedule::getName, name);
        return scheduleMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.SCHEDULE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Schedule> list(ScheduleDto scheduleDto) {
        if (!Optional.ofNullable(scheduleDto.getPage()).isPresent()) {
            scheduleDto.setPage(new Pages());
        }
        return scheduleMapper.selectPage(scheduleDto.getPage().convert(), fuzzyQuery(scheduleDto));
    }

    @Override
    @Cacheable(value = Common.Cache.SCHEDULE + Common.Cache.DIC, key = "'schedule_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        return null;
    }

    @Override
    public LambdaQueryWrapper<Schedule> fuzzyQuery(ScheduleDto scheduleDto) {
        LambdaQueryWrapper<Schedule> queryWrapper = Wrappers.<Schedule>query().lambda();
        Optional.ofNullable(scheduleDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Schedule::getName, dto.getName());
            }
            if (null != dto.getStatus()) {
                queryWrapper.eq(Schedule::getStatus, dto.getStatus());
            }
            if (null != dto.getDeviceId()) {
                queryWrapper.eq(Schedule::getDeviceId, dto.getDeviceId());
            }
        });
        return queryWrapper;
    }

}
