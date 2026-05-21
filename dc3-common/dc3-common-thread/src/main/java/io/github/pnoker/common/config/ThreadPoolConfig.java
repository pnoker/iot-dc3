/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.config;

import io.github.pnoker.common.thread.entity.property.ThreadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread Pool Configuration Class
 * <p>
 * Configuration class for creating and managing thread pools in Spring Boot applications.
 * Provides three types of thread pools: standard ThreadPoolExecutor, virtual thread pool,
 * and ScheduledThreadPoolExecutor with custom rejection policies.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final AtomicInteger threadPoolAtomic = new AtomicInteger(1);

    private final AtomicInteger scheduledThreadPoolAtomic = new AtomicInteger(1);

    private final ThreadProperties thread;

    /**
     * Create ThreadPoolExecutor with LinkedBlockingQueue
     *
     * @return Configured ThreadPoolExecutor bean
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(thread.getCorePoolSize(), thread.getMaximumPoolSize(), thread.getKeepAliveTime(),
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(thread.getMaximumPoolSize() * 2),
                r -> new Thread(r, "[T]" + thread.getPrefix() + threadPoolAtomic.getAndIncrement()),
                new BlockingRejectedExecutionHandler());
    }

    /**
     * Create virtual thread executor service Creates one virtual thread per task for
     * improved resource utilization
     *
     * @return Virtual thread ExecutorService bean
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual().name("[VT]" + thread.getPrefix(), 0).factory();

        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * Create ScheduledThreadPoolExecutor for scheduled tasks
     *
     * @return Configured ScheduledThreadPoolExecutor bean
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        return new ScheduledThreadPoolExecutor(thread.getCorePoolSize(),
                r -> new Thread(r, "[ST]" + thread.getPrefix() + scheduledThreadPoolAtomic.getAndIncrement()),
                new BlockingRejectedExecutionHandler());
    }

    /**
     * Custom RejectedExecutionHandler for blocking rejected tasks
     * <p>
     * Instead of rejecting tasks when the thread pool is full, this handler attempts to
     * execute them in the calling thread.
     * </p>
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    private static class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {

        /**
         * Handle rejected execution by attempting to run task in calling thread
         *
         * @param runnable The runnable task requested to be executed
         * @param executor The executor attempting to execute this task
         */
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            try {
                log.info("BlockingRejectedExecutionHandler: {}", executor.toString());

                if (!executor.isShutdown()) {
                    runnable.run();
                }
            } catch (Exception e) {
                log.error("BlockingRejectedExecutionHandler failed", e);
            }
        }

    }

}
