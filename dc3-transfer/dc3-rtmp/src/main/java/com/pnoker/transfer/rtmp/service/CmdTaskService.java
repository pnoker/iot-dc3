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

import com.pnoker.transfer.rtmp.model.constant.Global;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 任务监听线程服务，从队列中取Cmd任务进行执行
 */
@Slf4j
public class CmdTaskService implements Runnable {
    @Override
    public void run() {
        log.info("启动 rtsp->rtmp 监听线程");
        while (true) {
            try {
                String taskId = Global.cmdTaskIdQueue.take();
                Global.taskMap.get(taskId).start();
                Thread.sleep(1000 * 5);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
