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

package com.dc3.common.sdk.config;

import com.dc3.common.bean.property.ThreadProperty;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 *
 * @author pnoker
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "server")
public class ThreadPoolConfig {
    @Setter
    private ThreadProperty thread;

    private final AtomicInteger threadPoolAtomic = new AtomicInteger(1);
    private final AtomicInteger scheduledThreadPoolAtomic = new AtomicInteger(1);

    /**
     * LinkedBlockingQueue ThreadPoolExecutor
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                thread.getCorePoolSize(),
                thread.getMaximumPoolSize(),
                thread.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(thread.getMaximumPoolSize() * 2),
                (r) -> new Thread(r, "[ThreadPoolExecutor]" + thread.getPrefix() + threadPoolAtomic.getAndIncrement()),
                (r, e) -> log.error("ThreadPoolExecutor Rejected"));
    }

    /**
     * ScheduledThreadPoolExecutor ThreadPoolExecutor
     */
    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        return new ScheduledThreadPoolExecutor(
                thread.getCorePoolSize(),
                (r) -> new Thread(r, "[ScheduledThreadPoolExecutor]" + thread.getPrefix() + scheduledThreadPoolAtomic.getAndIncrement()),
                (r, e) -> log.error("ScheduledThreadPoolExecutor Rejected"));
    }

}
