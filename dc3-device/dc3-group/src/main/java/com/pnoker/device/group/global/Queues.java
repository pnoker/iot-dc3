package com.pnoker.device.group.global;

import com.pnoker.device.group.model.wia.WiaData;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 全局 队列集合和线程池，用于存放线程队列、数据队列以及线程池
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
    public static LinkedBlockingQueue<WiaData> wiaDataQueue = new LinkedBlockingQueue(2000);

    /**
     * 用于存放 wia 数据采集线程，为了性能，最多一次性放入32个
     */
    public static LinkedBlockingQueue<Runnable> wiaReceiveThreadQueue = new LinkedBlockingQueue(32);

    /**
     * 数据采集线程池，用于全部数采线程使用
     */
    public static ThreadPoolExecutor receivePoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, wiaReceiveThreadQueue, new ReceiveTreadFactory(), new IgnorePolicy());

    static class ReceiveTreadFactory implements ThreadFactory {
        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "dc3-group-thread-" + mThreadNum.getAndIncrement());
            log.info("{} has been created", thread.getName());
            return thread;
        }
    }

    static class IgnorePolicy implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            doLog(r, e);
        }

        private void doLog(Runnable r, ThreadPoolExecutor e) {
            log.error("{} rejected,completedTaskCount:{}", r.toString(), e.getCompletedTaskCount());
        }
    }

}
