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

package com.pnoker.transfer.rtmp.service;

import com.pnoker.transfer.rtmp.constant.Global;
import com.pnoker.transfer.rtmp.model.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Process输出处理线程服务
 */
@Data
@Slf4j
public class OutputService implements Runnable {
    /**
     * 任务 ID
     */
    private String id;
    private volatile boolean status = true;
    private Process process;

    public OutputService(String id, Process process) {
        this.id = id;
        this.process = process;
    }

    @Override
    public void run() {
        read(process.getErrorStream());
    }

    /**
     * 读取 CMD 执行输出
     *
     * @param inputStream
     */
    public void read(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while (null != (line = reader.readLine()) && status) {
                log.debug(line);
                handle(line.toLowerCase());
            }
            if (!status) {
                Global.taskMap.get(id).clear();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            reStartTask();
        }
    }

    /**
     * 通过输出内容检测是否运行错误，从而判断是否需要重启 CMD 任务
     *
     * @param message
     */
    public void handle(String message) {
        if (message.contains("fail") || message.contains("miss") || message.contains("error")) {
            log.error(message);
            log.info("准备重启 task {}", id);
            status = false;
            reStartTask();
        }
    }

    /**
     * 重启错误 CMD 任务
     */
    public void reStartTask() {
        Task task = Global.taskMap.get(id);
        task.clear();
        task.create();
        task.setStatus(2);
    }

}
