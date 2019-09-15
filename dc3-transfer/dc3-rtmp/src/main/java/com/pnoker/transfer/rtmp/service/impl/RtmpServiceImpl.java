/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.transfer.rtmp.service.impl;

import com.pnoker.common.model.domain.rtmp.Rtmp;
import com.pnoker.common.model.dto.Response;
import com.pnoker.common.utils.Tools;
import com.pnoker.transfer.rtmp.feign.RtmpDbsFeignApi;
import com.pnoker.transfer.rtmp.model.constant.Global;
import com.pnoker.transfer.rtmp.model.dto.CmdTask;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.getProperty;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Service
@ConfigurationProperties(prefix = "ffmpeg")
public class RtmpServiceImpl implements RtmpService {
    private String unix;
    private String window;

    private volatile int times = 1;

    @Autowired
    private RtmpDbsFeignApi rtmpDbsFeignApi;

    @Override
    public List<Rtmp> getRtmpList() {
        Response<List<Rtmp>> response = rtmpDbsFeignApi.list();
        if (!response.isOk() || times == 1) {
            times = 2;
            response.setData(null);
            log.error(response.getMessage());
            return reconnect();
        }
        List<Rtmp> list = response.getData();
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public boolean createCmdTask(Rtmp rtmp) {
        String ffmpeg = getProperty("os.name").toLowerCase().startsWith("win") ? window : unix;
        if ("".equals(ffmpeg) || null == ffmpeg) {
            log.error("FFmpeg path is NULL !");
            return false;
        }
        if (!Tools.isFile(ffmpeg)) {
            log.error("{} does not exist", ffmpeg);
            return false;
        }

        String cmd = rtmp.getCommand()
                .replace("{exe}", ffmpeg)
                .replace("{rtsp_url}", rtmp.getRtspUrl())
                .replace("{rtmp_url}", rtmp.getRtmpUrl());
        return createCmdTask(new CmdTask(cmd));
    }

    @Override
    public boolean stopCmdTask(String id) {
        Global.taskMap.get(id).stop();
        return false;
    }

    /**
     * 创建视频转码任务
     *
     * @param cmdTask
     */
    public static boolean createCmdTask(CmdTask cmdTask) {
        // 判断任务是否被重复提交
        if (!Global.taskMap.containsKey(cmdTask.getId())) {
            if (Global.taskMap.size() <= Global.MAX_TASK_SIZE) {
                Global.taskMap.put(cmdTask.getId(), cmdTask);
                return cmdTask.create();
            } else {
                log.error("超过最大任务数 {}", Global.MAX_TASK_SIZE);
                return false;
            }
        } else {
            log.error("重复任务 {}", cmdTask.getId());
            return false;
        }
    }

    public List<Rtmp> reconnect() {
        // N 次重连机会
        if (times > Global.CONNECT_MAX_TIMES) {
            log.info("一共重连 {} 次,无法连接数据库服务,服务停止！", times);
            System.exit(1);
        }
        log.info("第 {} 次重连", times);
        times++;
        try {
            Thread.sleep(Global.CONNECT_INTERVAL * times);
        } catch (Exception e) {
        }
        return getRtmpList();
    }
}
