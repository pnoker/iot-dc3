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

package com.dc3.transfer.rtmp.service.pool;

import com.dc3.transfer.rtmp.bean.ThreadProperty;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@Configuration
@ConfigurationProperties(prefix = "server")
public class ThreadPool {
    @Setter
    private ThreadProperty thread;

    private final AtomicInteger atomicInteger = new AtomicInteger(1);

    /**
     * thread pool
     */
    @Bean
    public ThreadPoolExecutor poolExecutor() {
        return new ThreadPoolExecutor(thread.getCorePoolSize(), thread.getMaximumPoolSize(), thread.getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(thread.getMaximumPoolSize() * 2),
                r -> {
                    Thread mThread = new Thread(r, thread.getPrefix() + atomicInteger.getAndIncrement());
                    log.debug("Create thread {}", mThread.getName());
                    return mThread;
                },
                (r, e) -> log.error("thread pool rejected"));
    }
}
