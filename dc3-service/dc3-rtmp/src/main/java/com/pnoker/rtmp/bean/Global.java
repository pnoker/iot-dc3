package com.pnoker.rtmp.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Global，用于存储Task队列和任务信息
 */
public class Global {
    public static Map<String, Tasker> taskMap = new HashMap<>(2);
    public static LinkedBlockingQueue<Tasker> taskQueue = new LinkedBlockingQueue(20);
}
