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
package com.pnoker.rtmp.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.rtmp.bean.Global;
import com.pnoker.rtmp.bean.Task;
import com.pnoker.rtmp.bean.Cmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rest接口控制器
 */
@Slf4j
@RestController
public class IndexController extends BaseController {

    @GetMapping("/test")
    public String hello() {
        Cmd cmd = new Cmd("D:/Documents/FFmpeg/bin/");
        cmd.create("ffmpeg")
                .add("-i", "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov")
                .add("-vcodec", "copy")
                .add("-acodec", "copy")
                .add("-f", "flv")
                .add("-y", "rtmp://114.116.9.76:1935/rtmp/bigbuckbunny_175k").build();
        Task task = new Task(UUID.randomUUID().toString(), cmd.getCmd());
        try {
            Global.taskQueue.put(task);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return "ok";
    }

}
