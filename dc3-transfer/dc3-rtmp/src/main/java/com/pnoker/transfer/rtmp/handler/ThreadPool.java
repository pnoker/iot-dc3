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

package com.pnoker.transfer.rtmp.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Global，用于存储Task队列和任务信息
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
public class ThreadPool {

    /**
     * Cmd任务线程池，用于全部任务线程使用
     */
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Property.CORE_POOL_SIZE, Property.MAX_POOL_SIZE, Property.KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(Property.MAX_POOL_SIZE * 2), new RtmpTaskTreadFactory(), new IgnorePolicy());

    static class RtmpTaskTreadFactory implements ThreadFactory {
        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "dc3-rtmp-thread-" + mThreadNum.getAndIncrement());
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
