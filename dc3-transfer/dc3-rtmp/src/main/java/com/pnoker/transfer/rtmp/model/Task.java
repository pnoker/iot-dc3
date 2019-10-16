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

package com.pnoker.transfer.rtmp.model;

import com.pnoker.common.utils.Tools;
import com.pnoker.transfer.rtmp.constant.Global;
import com.pnoker.transfer.rtmp.service.OutputService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <p>Command 指令执行任务信息类
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@Slf4j
public class Task {
    private String id;

    /**
     * 任务运行状态
     * <p>
     * 0：初始化完成，等待被启动 <br>
     * 1：启动完毕，正在运行中 <br>
     * 2：任务错误，等待被重启 <br>
     * 3：多次重启失败，任务已停止 <br>
     * 4：任务停止 <br>
     */
    private int status = 0;

    /**
     * 任务累计被启动次数
     */
    private int startTimes = 0;

    private String command;
    private Process process;
    private OutputService outputService;

    /**
     * 构造函数
     *
     * @param command
     */
    public Task(String command) {
        this.id = Tools.uuid();
        this.status = 0;
        this.command = command;
    }

    /**
     * 启动 CMD 任务
     * 当任务启动次数超过最大次数，该任务不再被执行
     */
    public void start() {
        if (startTimes < Global.MAX_TASK_TIMES) {
            try {
                log.info("启动 task->{} , command->{}", id, command);
                status = 1;
                process = Runtime.getRuntime().exec(command);
                startTimes++;
                outputService = new OutputService(id, process);
                Global.threadPoolExecutor.execute(outputService);
            } catch (IOException e) {
                status = 2;
                clear();
                create();
                log.error(e.getMessage(), e);
            }
        } else {
            clear();
            status = 3;
            log.error("任务 {} 达到最大重启次数，该任务不再执行", id);
        }
    }

    /**
     * 停止执行 CMD 任务
     */
    public boolean stop() {
        status = 4;
        if (null != outputService) {
            if (outputService.isStatus()) {
                outputService.setStatus(false);
                return true;
            }
        }
        return false;
    }

    /**
     * 将任务放入队列，创建一个待启动的任务
     */
    public boolean create() {
        if (!Global.cmdTaskIdQueue.offer(id)) {
            log.error("Current tasks queue is full,please try again later.");
            return false;
        }
        status = 0;
        return true;
    }

    /**
     * 清理 Process 和 OutputService
     */
    public void clear() {
        if (null != process) {
            process.destroyForcibly();
            process = null;
        }
        outputService = null;
    }
}
