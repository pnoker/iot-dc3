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
package com.pnoker.transfer.rtmp.bean;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Global，用于存储Task队列和任务信息
 */
@Slf4j
public class Global {
    public static Map<String, Task> taskMap = new HashMap<>(20);
    public static LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue(20);

    public static void putTask(Task task) {
        taskMap.put(task.getTaskId(), task);
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
