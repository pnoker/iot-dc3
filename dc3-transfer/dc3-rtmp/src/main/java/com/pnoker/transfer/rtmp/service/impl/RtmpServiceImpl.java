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

package com.pnoker.transfer.rtmp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.RtmpDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Rtmp;
import com.pnoker.transfer.rtmp.handler.Transcode;
import com.pnoker.transfer.rtmp.handler.TranscodePool;
import com.pnoker.transfer.rtmp.mapper.RtmpMapper;
import com.pnoker.transfer.rtmp.service.RtmpService;
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
 * @author pnoker
 */
@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {

    @Resource
    private RtmpMapper rtmpMapper;

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.RTMP + Common.Cache.ID, key = "#rtmp.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Rtmp add(Rtmp rtmp) {
        if (rtmpMapper.insert(rtmp) > 0) {
            Transcode transcode = new Transcode(rtmp);
            if (!TranscodePool.transcodeMap.containsKey(transcode.getId())) {
                TranscodePool.transcodeMap.put(transcode.getId(), transcode);
                return selectById(rtmp.getId());
            }
            throw new ServiceException("任务重复,表记录添加成功");
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        Rtmp select = selectById(id);
        if (null != select) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    throw new ServiceException("任务运行中");
                }
                TranscodePool.transcodeMap.remove(id);
            }
            if (rtmpMapper.deleteById(id) > 0) {
                return true;
            }
            throw new ServiceException("任务删除成功,表记录删除失败");
        }
        throw new ServiceException("任务不存在");
    }

    @Override
    @Caching(
            put = {@CachePut(value = Common.Cache.RTMP + Common.Cache.ID, key = "#rtmp.id", condition = "#result!=null")},
            evict = {
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Rtmp update(Rtmp rtmp) {
        rtmp.setUpdateTime(null);
        Rtmp select = selectById(rtmp.getId());
        if (null != select) {
            Transcode transcode = TranscodePool.transcodeMap.get(rtmp.getId());
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    throw new ServiceException("任务运行中");
                }
                TranscodePool.transcodeMap.put(transcode.getId(), new Transcode(rtmp));
            }
            if (rtmpMapper.updateById(rtmp) > 0) {
                return select;
            }
            throw new ServiceException("任务更新成功,表记录更新失败");
        }
        throw new ServiceException("任务不存在");
    }

    @Override
    @Cacheable(value = Common.Cache.RTMP + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Rtmp selectById(Long id) {
        return rtmpMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.RTMP + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Rtmp> list(RtmpDto rtmpDto) {
        return rtmpMapper.selectPage(rtmpDto.getPage().convert(), fuzzyQuery(rtmpDto));
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean start(Long id) {
        Rtmp select = rtmpMapper.selectById(id);
        if (null != select) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    throw new ServiceException("任务已是启动状态");
                }
            } else {
                transcode = new Transcode(select);
                TranscodePool.transcodeMap.put(transcode.getId(), transcode);
            }
            TranscodePool.threadPoolExecutor.execute(() -> TranscodePool.transcodeMap.get(id).start());
            if (rtmpMapper.updateById(select.setRun(true)) > 0) {
                return true;
            }
            throw new ServiceException("任务启动成功，表记录更新失败");
        }
        throw new ServiceException("任务不存在");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.RTMP + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean stop(Long id) {
        Rtmp select = rtmpMapper.selectById(id);
        if (null != select) {
            Transcode transcode = TranscodePool.transcodeMap.get(id);
            if (Optional.ofNullable(transcode).isPresent()) {
                if (transcode.isRun()) {
                    transcode.stop();
                    if (rtmpMapper.updateById(select.setRun(false)) > 0) {
                        return true;
                    }
                    throw new ServiceException("任务停止成功，表记录更新失败");
                }
            }
            throw new ServiceException("任务已是停止状态");
        }
        throw new ServiceException("任务不存在");
    }

    @Override
    public LambdaQueryWrapper<Rtmp> fuzzyQuery(RtmpDto rtmpDto) {
        LambdaQueryWrapper<Rtmp> queryWrapper = Wrappers.<Rtmp>query().lambda();
        Optional.ofNullable(rtmpDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Rtmp::getName, dto.getName());
            }
            if (null != rtmpDto.getAutoStart()) {
                queryWrapper.eq(Rtmp::getAutoStart, dto.getAutoStart());
            }
        });
        return queryWrapper;
    }

}
