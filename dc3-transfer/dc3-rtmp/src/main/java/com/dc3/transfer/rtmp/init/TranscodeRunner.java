/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.transfer.rtmp.init;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.common.bean.Pages;
import com.dc3.common.dto.RtmpDto;
import com.dc3.common.model.Rtmp;
import com.dc3.transfer.rtmp.service.RtmpService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.System.getProperty;

/**
 * 启动服务，自动加载自启任务
 *
 * @author pnoker
 */
@Slf4j
@Setter
@Order(10)
@Component
public class TranscodeRunner implements ApplicationRunner {
    public static String ffmpeg;

    @Value("${rtmp.ffmpeg.window}")
    private String window;
    @Value("${rtmp.ffmpeg.unix}")
    private String unix;

    @Resource
    private RtmpService rtmpService;

    @Override
    public void run(ApplicationArguments args) {
        ffmpeg = getProperty("os.name").toLowerCase().startsWith("win") ? window : unix;
        if (StringUtils.isBlank(ffmpeg)) {
            log.error("FFmpeg path is null,Please fill absolute path!");
            System.exit(1);
        }
        if (!FileUtil.isFile(ffmpeg)) {
            log.error("{} does not exist,Please fill absolute path!", ffmpeg);
            System.exit(1);
        }
        authStartList().forEach(rtmp -> rtmpService.start(rtmp.getId()));
    }

    public List<Rtmp> authStartList() {
        Page<Rtmp> page = rtmpService.list(new RtmpDto(true).setPage(new Pages().setSize(-1L)));
        return Optional.ofNullable(page.getRecords()).orElse(new ArrayList<>());
    }

}
