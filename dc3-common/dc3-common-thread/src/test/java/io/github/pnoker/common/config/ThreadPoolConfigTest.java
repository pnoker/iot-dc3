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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ThreadPoolConfigTest {

    private ThreadProperties properties;
    private ThreadPoolConfig config;
    private ThreadPoolExecutor pool;
    private ExecutorService virtual;
    private ScheduledThreadPoolExecutor scheduled;

    @BeforeEach
    void setUp() {
        properties = new ThreadProperties();
        properties.setPrefix("test-thread-");
        properties.setCorePoolSize(2);
        properties.setMaximumPoolSize(4);
        properties.setKeepAliveTime(15);
        config = new ThreadPoolConfig(properties);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdownNow();
        }
        if (virtual != null) {
            virtual.shutdownNow();
        }
        if (scheduled != null) {
            scheduled.shutdownNow();
        }
    }

    @Test
    void threadPoolExecutorRespectsCoreAndMaxSizeAndQueueDepth() {
        pool = config.threadPoolExecutor();
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
        assertThat(pool.getQueue()).isInstanceOf(LinkedBlockingQueue.class);
        // Queue capacity is 2 * maximumPoolSize per the config — verify by adding tasks
        // that would otherwise exhaust the queue.
        assertThat(((LinkedBlockingQueue<?>) pool.getQueue()).remainingCapacity()).isEqualTo(8);
    }

    @Test
    void threadPoolUsesPrefixedThreadNames() throws Exception {
        pool = config.threadPoolExecutor();
        java.util.concurrent.atomic.AtomicReference<String> threadName = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        pool.submit(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        assertThat(threadName.get()).startsWith("[T]test-thread-");
    }

    @Test
    void blockingRejectedHandlerRunsInCallerThreadWhenQueueIsFull() throws Exception {
        // Saturate the pool: 2 cores busy + 8 queued + 2 max = up to 12 simultaneous; 13th
        // should be rejected and run in the caller thread.
        pool = config.threadPoolExecutor();
        java.util.concurrent.CountDownLatch hold = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(4);
        for (int i = 0; i < 12; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    hold.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        // 13th task: pool full + queue full -> rejected handler runs it on caller thread.
        AtomicInteger ranOn = new AtomicInteger();
        Thread caller = Thread.currentThread();
        pool.execute(() -> ranOn.set(Thread.currentThread() == caller ? 1 : 0));
        // Give the rejected handler time to run.
        await().atMost(java.time.Duration.ofSeconds(2)).until(() -> ranOn.get() != 0);
        assertThat(ranOn.get()).isEqualTo(1);
        hold.countDown();
    }

    @Test
    void virtualThreadExecutorProducesVirtualThreads() throws Exception {
        virtual = config.virtualThreadExecutor();
        java.util.concurrent.atomic.AtomicReference<Thread> seen = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        virtual.submit(() -> {
            seen.set(Thread.currentThread());
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        assertThat(seen.get().isVirtual()).isTrue();
        assertThat(seen.get().getName()).startsWith("[VT]test-thread-");
    }

    @Test
    void scheduledExecutorRespectsCoreSize() {
        scheduled = config.scheduledThreadPoolExecutor();
        assertThat(scheduled.getCorePoolSize()).isEqualTo(2);
    }

    @Test
    void scheduledExecutorUsesPrefixedThreadNames() throws Exception {
        scheduled = config.scheduledThreadPoolExecutor();
        java.util.concurrent.atomic.AtomicReference<String> threadName = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        scheduled.schedule(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);
        latch.await(2, TimeUnit.SECONDS);
        assertThat(threadName.get()).startsWith("[ST]test-thread-");
    }
}
