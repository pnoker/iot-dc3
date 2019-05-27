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
package com.pnoker.rtmp.handle;

import com.pnoker.rtmp.bean.Global;
import com.pnoker.rtmp.bean.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Process输出处理线程
 */
@Slf4j
@Data
public class OutputHandle implements Runnable {
    private String taskId;
    /**
     * 运行状态，false：运行失败
     */
    private volatile boolean status = true;
    private Process process;

    public OutputHandle(String taskId, Process process) {
        this.taskId = taskId;
        this.process = process;
    }

    @Override
    public void run() {
        if (status) {
            read(process.getErrorStream());
        }
    }

    public void read(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while (null != (line = reader.readLine())) {
                log.info(line);
                handle(line);
                if (!status) {
                    throw new IOException();
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
                process.destroyForcibly();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void handle(String message) {
        message = message.toLowerCase();
        if (message.contains("fail") || message.contains("miss")) {
            this.status = false;
            Task task = Global.taskMap.get(taskId);
            task.setTimes(task.getTimes() + 1);
            task.setStatus(3);
            try {
                Global.taskQueue.put(task);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
