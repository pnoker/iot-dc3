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

package com.pnoker.transfer.rtmp.runner;

import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.handle.TaskHandle;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.System.getProperty;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 启动服务，自动加载自启任务
 */
@Slf4j
@Setter
@Order(1)
@Component
@ConfigurationProperties(prefix = "ffmpeg")
public class TaskRunner implements ApplicationRunner {

    private String unix;
    private String window;

    @Autowired
    private RtmpService rtmpService;

    @Override
    public void run(ApplicationArguments args) {

        log.info("Prepare to start rtsp->rtmp thread");
        String ffmpeg = getProperty("os.name").toLowerCase().startsWith("win") ? window : unix;
        List<Rtmp> list = rtmpService.getRtmpList();
        for (Rtmp rtmp : list) {
            rtmpService.createTask(rtmp, ffmpeg);
        }
        new Thread(new TaskHandle()).start();
    }

}
