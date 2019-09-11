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

import com.pnoker.common.bean.base.Response;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.bean.CmdTask;
import com.pnoker.transfer.rtmp.constant.Global;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {
    private volatile int times = 1;

    @Autowired
    private RtmpFeignApi rtmpFeignApi;

    @Override
    public List<Rtmp> getRtmpList() {
        Map<String, Object> condition = new HashMap<>(2);
        condition.put("auto_start", false);
        Response<List<Rtmp>> response = rtmpFeignApi.list();
        if (!response.isOk()) {
            log.error(response.getMessage());
            reconnect();
        }
        List<Rtmp> list = response.getData();
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public boolean createTask(Rtmp rtmp, String ffmpeg) {
        String cmd = rtmp.getCommand()
                .replace("{exe}", ffmpeg)
                .replace("{rtsp_url}", rtmp.getRtspUrl())
                .replace("{rtmp_url}", rtmp.getRtmpUrl());
        return Global.createTask(new CmdTask(cmd));
    }

    public void reconnect() {
        // N 次重连机会
        if (times > Global.CONNECT_MAX_TIMES) {
            log.info("一共重连 {} 次,退出重连,服务停止！", times);
            System.exit(1);
        }
        log.info("第 {} 次重连", times);
        times++;
        try {
            Thread.sleep(Global.CONNECT_INTERVAL * times);
            getRtmpList();
        } catch (Exception e) {
        }
    }
}
