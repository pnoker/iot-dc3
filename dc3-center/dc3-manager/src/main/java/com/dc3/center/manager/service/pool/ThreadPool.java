/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.center.manager.service.pool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pnoker
 */
@Slf4j
@Component
public class ThreadPool {
    public static int CORE_POOL_SIZE = 4;

    private final AtomicInteger atomicInteger = new AtomicInteger(1);

    /**
     * 线程池
     */
    public ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, r -> {
        Thread thread = new Thread(r, "dc3-manager-thread-" + atomicInteger.getAndIncrement());
        log.debug("Create thread {}", thread.getName());
        return thread;
    }, (r, e) -> log.error("thread pool rejected"));
}
