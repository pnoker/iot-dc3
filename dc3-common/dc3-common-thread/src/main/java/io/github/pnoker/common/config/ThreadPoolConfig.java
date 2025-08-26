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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用线程池配置
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    private final AtomicInteger threadPoolAtomic = new AtomicInteger(1);
    private final AtomicInteger scheduledThreadPoolAtomic = new AtomicInteger(1);

    private final ThreadProperties thread;

    public ThreadPoolConfig(ThreadProperties thread) {
        this.thread = thread;
    }

    /**
     * LinkedBlockingQueue ThreadPoolExecutor
     *
     * @return ThreadPoolExecutor
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                thread.getCorePoolSize(),
                thread.getMaximumPoolSize(),
                thread.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(thread.getMaximumPoolSize() * 2),
                r -> new Thread(r, "[T]" + thread.getPrefix() + threadPoolAtomic.getAndIncrement()),
                (r, e) -> new BlockingRejectedExecutionHandler());
    }

    /**
     * ScheduledThreadPoolExecutor ThreadPoolExecutor
     *
     * @return ScheduledThreadPoolExecutor
     */
    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        return new ScheduledThreadPoolExecutor(
                thread.getCorePoolSize(),
                r -> new Thread(r, "[S]" + thread.getPrefix() + scheduledThreadPoolAtomic.getAndIncrement()),
                (r, e) -> new BlockingRejectedExecutionHandler());
    }

    /**
     * 自定义 RejectedExecutionHandler
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    private static class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            try {
                log.info("BlockingRejectedExecutionHandler: {}", executor.toString());

                if (!executor.isShutdown()) {
                    runnable.run();
                }
            } catch (Exception e) {
                log.error("BlockingRejectedExecutionHandler: {}", e.getMessage(), e);
            }
        }
    }

}
