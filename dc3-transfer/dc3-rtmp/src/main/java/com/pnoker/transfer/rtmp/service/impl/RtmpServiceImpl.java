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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.dbs.rtmp.feign.RtmpDbsFeignApi;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.bean.Response;
import com.pnoker.transfer.rtmp.handler.Task;
import com.pnoker.transfer.rtmp.runner.TranscodeRunner;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {

    private volatile int times = 1;

    @Autowired
    private RtmpDbsFeignApi rtmpDbsFeignApi;

    @Override
    public List<Rtmp> getRtmpList(RtmpDto rtmp) {
        rtmp.setName("测试");
        Response<Page<Rtmp>> response = rtmpDbsFeignApi.list(rtmp);
        if (!response.isOk()) {
            log.error(response.getMessage());
            return reconnect();
        }
        List<Rtmp> list = response.getData().getRecords();

        return list != null ? list : new ArrayList<>();
    }

    @Override
    public Response addRtmp(Rtmp rtmp) {
        return rtmpDbsFeignApi.add(rtmp);
    }

    @Override
    public Response startTask(Rtmp rtmp) {
        Task task = new Task(getCommand(rtmp));
        // 判断任务是否被重复提交
        if (!Task.taskMap.containsKey(task.getId())) {
            if (Task.taskMap.size() <= TranscodeRunner.taskMaxSize) {
                Task.taskMap.put(task.getId(), task);
                return task.create() ? Response.ok() : Response.fail();
            } else {
                log.error("超过最大任务数 {}", TranscodeRunner.taskMaxSize);
                return Response.fail("超过最大任务数");
            }
        } else {
            log.error("重复任务 {}", task.getId());
            return Response.fail("重复任务");
        }
    }

    @Override
    public Response stopTask(String id) {
        Task.taskMap.get(id).stop();
        return Response.fail();
    }


    public String getCommand(Rtmp rtmp) {
        String cmd = rtmp.getCommand()
                .replace("{exe}", TranscodeRunner.ffmpeg)
                .replace("{rtsp_url}", rtmp.getRtspUrl())
                .replace("{rtmp_url}", rtmp.getRtmpUrl());
        return cmd;
    }

    public List<Rtmp> reconnect() {
        // N 次重连机会
        if (times > TranscodeRunner.reconnectMaxTimes) {
            log.info("一共重连 {} 次,无法连接数据库服务,服务停止！", times);
            System.exit(1);
        }
        log.info("第 {} 次重连", times);
        times++;
        try {
            // 设置重连之间的间隔时间
            Thread.sleep(TranscodeRunner.reconnectInterval * times);
        } catch (Exception e) {
        }
        return getRtmpList(new RtmpDto(true));
    }
}
