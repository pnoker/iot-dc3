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

package com.dc3.transfer.rtmp.bean;

import cn.hutool.core.util.RuntimeUtil;
import com.dc3.common.model.Rtmp;
import com.dc3.transfer.rtmp.init.TranscodeRunner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Optional;

/**
 * Command 指令执行任务信息类
 *
 * @author pnoker
 */
@Data
@Slf4j
public class Transcode {
    private Long id;
    private volatile boolean run;
    private String command;
    private Process process;

    public Transcode(Rtmp rtmp) {
        this.id = rtmp.getId();
        this.run = false;
        this.command = rtmp.getCommand()
                .replace("{exe}", TranscodeRunner.ffmpeg)
                .replace("{rtsp_url}", rtmp.getRtspUrl())
                .replace("{rtmp_url}", rtmp.getRtmpUrl());
    }

    public void start() {
        run = true;
        process = RuntimeUtil.exec(command);
        InputStream inputStream = process.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while (StringUtils.isNotEmpty((line = reader.readLine())) && run) {
                log.error(line);
                line = line.toLowerCase();
                if (line.contains("fail") || line.contains("error")) {
                    stop();
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
    }

    public void stop() {
        run = false;
        Optional.ofNullable(process).ifPresent(process -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            try {
                writer.write("q");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            process.destroyForcibly();
        });
    }

}
