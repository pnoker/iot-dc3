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

import com.pnoker.transfer.rtmp.runner.Environment;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 转码任务执行线程池
 *
 * @author pnoker
 */
@Slf4j
public class TranscodePool {
    private static final AtomicInteger mThreadNum = new AtomicInteger(1);

    /**
     * 转码任务Map
     */
    public static volatile Map<Long, Transcode> transcodeMap = new HashMap<>(64);

    /**
     * 线程池
     */
    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Environment.CORE_POOL_SIZE,
            Environment.MAX_POOL_SIZE, Environment.KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(Environment.MAX_POOL_SIZE * 2),
            r -> {
                Thread thread = new Thread(r, "dc3-thread-" + mThreadNum.getAndIncrement());
                log.info("{} has been created", thread.getName());
                return thread;
            },
            (r, e) -> log.error("{} rejected,completedTaskCount:{}", r.toString(), e.getCompletedTaskCount()));
}
