/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.transfer.rtmp.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dto.RtmpDto;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Rtmp;
import io.github.pnoker.transfer.rtmp.bean.Transcode;
import io.github.pnoker.transfer.rtmp.mapper.RtmpMapper;
import io.github.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {
    /**
     * 转码任务Map
     */
    public volatile Map<String, Transcode> transcodeMap = new ConcurrentHashMap<>(16);

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private RtmpMapper rtmpMapper;

    @Override
    public Rtmp add(Rtmp rtmp) {
        if (rtmpMapper.insert(rtmp) > 0) {
            Transcode transcode = new Transcode(rtmp);
            if (!transcodeMap.containsKey(transcode.getId())) {
                transcodeMap.put(transcode.getId(), transcode);
            }
            return selectById(rtmp.getId());
        }
        throw new ServiceException("The rtmp task add failed");
    }

    @Override
    public boolean delete(String id) {
        Rtmp select = selectById(id);
        if (ObjectUtil.isNotNull(select)) {
            Transcode transcode = transcodeMap.get(id);
            if (ObjectUtil.isNotNull(transcode)) {
                if (transcode.isRun()) {
                    throw new ServiceException("The rmp task is running");
                }
                transcodeMap.remove(id);
            }
            return rtmpMapper.deleteById(id) > 0;
        }
        throw new NotFoundException("The rtmp task does not exist");
    }

    @Override
    public Rtmp update(Rtmp rtmp) {
        rtmp.setUpdateTime(null);
        Rtmp select = selectById(rtmp.getId());
        if (null == select) {
            throw new NotFoundException("The rtmp task does not exist");
        }
        Transcode transcode = transcodeMap.get(rtmp.getId());
        if (ObjectUtil.isNotNull(transcode)) {
            if (transcode.isRun()) {
                throw new ServiceException("The rtmp task is running");
            }
            transcodeMap.put(transcode.getId(), new Transcode(rtmp));
        }
        if (rtmpMapper.updateById(rtmp) > 0) {
            return rtmp;
        }
        throw new ServiceException("The rtmp task update failed");
    }

    @Override
    public Rtmp selectById(String id) {
        return rtmpMapper.selectById(id);
    }

    @Override
    public Page<Rtmp> list(RtmpDto rtmpDto) {
        return rtmpMapper.selectPage(rtmpDto.getPage().convert(), fuzzyQuery(rtmpDto));
    }

    @Override
    public boolean start(String id) {
        Rtmp select = rtmpMapper.selectById(id);
        if (null == select) {
            throw new NotFoundException("The rtmp task does not exist");
        }

        Transcode transcode = transcodeMap.get(id);
        if (null == transcode) {
            transcode = new Transcode(select);
            transcodeMap.put(transcode.getId(), transcode);
        }

        if (transcode.isRun()) {
            throw new ServiceException("The rtmp task is running");
        }

        // 设置转码任务状态
        transcode.setRun(true);
        transcode.setProcess(RuntimeUtil.exec(transcode.getCommand()));

        // 打印常规输出
        threadPoolExecutor.execute(() -> {
            Transcode temp = transcodeMap.get(id);
            InputStream inputStream = temp.getProcess().getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            try {
                while (StrUtil.isNotEmpty((line = bufferedReader.readLine())) && temp.isRun()) {
                    log.debug(line);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        // 打印错误输出
        threadPoolExecutor.execute(() -> {
            Transcode temp = transcodeMap.get(id);
            InputStream inputStream = temp.getProcess().getErrorStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            try {
                while (StrUtil.isNotEmpty((line = bufferedReader.readLine())) && temp.isRun()) {
                    log.error(line);
                    line = line.toLowerCase();
                    if (line.contains("fail") || line.contains("error")) {
                        temp.quit();
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });


        if (rtmpMapper.updateById(select.setRun(true)) > 0) {
            return true;
        }
        throw new ServiceException("The rtmp task started successfully，database update failed");
    }

    @Override
    public boolean stop(String id) {
        Rtmp select = rtmpMapper.selectById(id);
        if (ObjectUtil.isNotNull(select)) {
            Transcode transcode = transcodeMap.get(id);
            if (ObjectUtil.isNotNull(transcode)) {
                if (transcode.isRun()) {
                    transcode.quit();
                    if (rtmpMapper.updateById(select.setRun(false)) > 0) {
                        return true;
                    }
                    throw new ServiceException("The rtmp task stop failed，database update failed");
                }
            }
            throw new ServiceException("The rtmp task is stopped");
        }
        throw new NotFoundException("The rtmp task does not exist");
    }

    @Override
    public LambdaQueryWrapper<Rtmp> fuzzyQuery(RtmpDto rtmpDto) {
        LambdaQueryWrapper<Rtmp> queryWrapper = Wrappers.<Rtmp>query().lambda();
        if (ObjectUtil.isNotNull(rtmpDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(rtmpDto.getName()), Rtmp::getName, rtmpDto.getName());
            queryWrapper.eq(ObjectUtil.isNotNull(rtmpDto.getAutoStart()), Rtmp::getAutoStart, rtmpDto.getAutoStart());
            queryWrapper.eq(StrUtil.isNotEmpty(rtmpDto.getTenantId()), Rtmp::getTenantId, rtmpDto.getTenantId());
        }
        return queryWrapper;
    }

}
