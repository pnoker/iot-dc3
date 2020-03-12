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

package com.github.pnoker.transfer.rtmp.service.pool;

import com.github.pnoker.transfer.rtmp.bean.Transcode;
import com.github.pnoker.transfer.rtmp.bean.RtmpProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
@Component
public class ThreadPool {
    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    /**
     * 转码任务Map
     */
    public volatile Map<Long, Transcode> transcodeMap = new ConcurrentHashMap<>(16);

    /**
     * 线程池
     */
    public ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(RtmpProperty.CORE_POOL_SIZE,
            RtmpProperty.MAX_POOL_SIZE, RtmpProperty.KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(RtmpProperty.MAX_POOL_SIZE * 2),
            r -> {
                Thread thread = new Thread(r, "dc3-rtmp-thread-" + mThreadNum.getAndIncrement());
                log.debug("{} has been created", thread.getName());
                return thread;
            },
            (r, e) -> log.error("thread pool rejected"));
}
