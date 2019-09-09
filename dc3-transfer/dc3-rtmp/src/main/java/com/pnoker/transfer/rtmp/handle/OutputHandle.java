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

package com.pnoker.transfer.rtmp.handle;

import com.pnoker.transfer.rtmp.bean.CmdTask;
import com.pnoker.transfer.rtmp.constant.Global;
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
 * <p>Description: Process输出处理线程
 */
@Slf4j
@Data
public class OutputHandle implements Runnable {
    private String taskId;
    private volatile boolean status = true;
    private Process process;

    public OutputHandle(String taskId, Process process) {
        this.taskId = taskId;
        this.process = process;
    }

    public void handle(String message) {
        if (message.contains("fail") || message.contains("miss") || message.contains("error")) {
            log.info("read to restart task {}", taskId);
            status = false;
            CmdTask cmdTask = Global.taskMap.get(taskId);
            cmdTask.setStartTimes(cmdTask.getStartTimes() + 1);
            cmdTask.setStatus(3);
            Global.createTask(cmdTask);
        }
    }

    public void read(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while (null != (line = reader.readLine()) && status) {
                log.info(line);
                handle(line.toLowerCase());
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

    @Override
    public void run() {
        read(process.getErrorStream());
    }
}
