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
package com.pnoker.rtmp.runner;

import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.utils.uid.UidTools;
import com.pnoker.rtmp.bean.Global;
import com.pnoker.rtmp.bean.Task;
import com.pnoker.rtmp.feign.RtmpFeignApi;
import com.pnoker.rtmp.handle.TaskHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 启动服务，自动加载自启任务
 */
@Slf4j
@Component
public class TaskRunner implements CommandLineRunner {

    private volatile int times = 0;
    private int reconnect = 5000;

    @Value("${ffmpeg.window}")
    private String window;
    @Value("${ffmpeg.nuix}")
    private String unix;

    @Autowired
    private RtmpFeignApi rtmpFeignApi;

    public List<Rtmp> getRtmpList() {
        List<Rtmp> list = new ArrayList<>();
        if (times > 10) {
            return list;
        }
        times++;
        try {
            Thread.sleep(reconnect * times);
        } catch (InterruptedException e) {
            log.error("{}", e.getMessage(), e);
        }
        list = rtmpFeignApi.list();
        if (list == null) {
            return getRtmpList();
        }
        return list;
    }

    @Override
    public void run(String... args) {
        log.info("ready to start rtsp->rtmp thread");
        String ffmpeg = getProperty("os.name").toLowerCase().startsWith("win") ? window : unix;
        List<Rtmp> list = getRtmpList();
        for (Rtmp rtmp : list) {
            String cmd = rtmp.getCommand()
                    .replace("{exe}", ffmpeg)
                    .replace("{rtsp_url}", rtmp.getRtspUrl())
                    .replace("{rtmp_url}", rtmp.getRtmpUrl());
            Task task = new Task(new UidTools().guid(), cmd);
            Global.putTask(task);
        }
        new Thread(new TaskHandle()).start();
    }
}
