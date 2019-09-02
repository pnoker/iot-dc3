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
package com.pnoker.transfer.rtmp.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.common.bean.base.Response;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.bean.CmdBuilder;
import com.pnoker.transfer.rtmp.bean.CmdTask;
import com.pnoker.transfer.rtmp.constant.Global;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rest接口控制器
 */
@Slf4j
@RestController
public class IndexController extends BaseController {
    @Autowired
    private RtmpFeignApi rtmpFeignApi;

    @GetMapping("/test")
    public String hello() {
        CmdBuilder cmdBuilder = new CmdBuilder("D:/Documents/FFmpeg/bin/");
        cmdBuilder.create("ffmpeg")
                .add("-i", "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov")
                .add("-vcodec", "copy")
                .add("-acodec", "copy")
                .add("-f", "flv")
                .add("-y", "rtmp://114.116.9.76:1935/rtmp/bigbuckbunny_175k").build();
        CmdTask cmdTask = new CmdTask(cmdBuilder.getCmd());
        try {
            Global.cmdTaskQueue.put(cmdTask);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return "ok";
    }

    @GetMapping("/list")
    public List<Rtmp> list() {
        Map<String, Object> condition = new HashMap<>(2);
        condition.put("auto_start", false);
        Response response = rtmpFeignApi.list("{}");
        List<Rtmp> list = (List<Rtmp>) response.getResult();
        return list;
    }

}
