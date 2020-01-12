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

package com.pnoker.device.group.constant;

import com.pnoker.device.group.model.wia.WiaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author pnoker
 */
@Slf4j
public class Queues {
    /**
     * 核心线程池大小
     */
    public static int corePoolSize = 4;

    /**
     * 最大线程池大小
     */
    public static int maximumPoolSize = 32;

    /**
     * 线程最大空闲时间
     */
    public static long keepAliveTime = 10;

    /**
     * 时间单位，秒
     */
    public static TimeUnit unit = TimeUnit.SECONDS;

    /**
     * 用于存放 wia 数据，支持2000并发，该队列用于优化大批量数据入库性能
     */
    public static LinkedBlockingQueue<WiaData> wiaDataQueue = new LinkedBlockingQueue<>(2000);

    /**
     * 用于缓存 wia 数据采集线程
     */
    public static LinkedBlockingQueue<Runnable> wiaReceiveThreadQueue = new LinkedBlockingQueue<>(64);

    /**
     * 数据采集线程池，用于全部数采线程使用
     */
    public static ThreadPoolExecutor receivePoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, wiaReceiveThreadQueue, new ReceiveTreadFactory(), new IgnorePolicy());

    static class ReceiveTreadFactory implements ThreadFactory {
        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, "dc3-group-thread-" + mThreadNum.getAndIncrement());
            log.info("{} has been created", thread.getName());
            return thread;
        }
    }

    static class IgnorePolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            doLog(r, e);
        }

        private void doLog(Runnable r, ThreadPoolExecutor e) {
            log.error("{} rejected,completedTaskCount:{}", r.toString(), e.getCompletedTaskCount());
        }
    }

}
