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

import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.utils.Dc3Tools;
import com.pnoker.transfer.rtmp.handler.Property;
import com.pnoker.transfer.rtmp.handler.Task;
import com.pnoker.transfer.rtmp.handler.ThreadPool;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.System.getProperty;

/**
 * <p>启动服务，自动加载自启任务
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Setter
@Order(1)
@Component
@ConfigurationProperties(prefix = "ffmpeg")
public class TranscodeApplicationRunner implements ApplicationRunner {
    private String unix;
    private String window;

    @Autowired
    private RtmpService rtmpService;

    @Override
    public void run(ApplicationArguments args) {
        String ffmpeg = getProperty("os.name").toLowerCase().startsWith("win") ? window : unix;
        if (StringUtils.isBlank(ffmpeg)) {
            log.error("FFmpeg path is null,Please fill absolute path!");
            System.exit(1);
        }
        if (!Dc3Tools.isFile(ffmpeg)) {
            log.error("{} does not exist,Please fill absolute path!", ffmpeg);
            System.exit(1);
        }
        Property.FFMPEG = ffmpeg;
        List<Rtmp> list = rtmpService.getRtmpList(new RtmpDto(true));
        for (Rtmp rtmp : list) {
            rtmpService.startTask(rtmp);
        }
        // 启动任务线程
        //todo 逻辑错误，没有使用线程池
        ThreadPool.threadPoolExecutor.execute(() -> {
            log.info("listening rtsp->rtmp task");
            while (true) {
                try {
                    Task.taskMap.get(Task.cmdTaskIdQueue.take()).start();
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

}
