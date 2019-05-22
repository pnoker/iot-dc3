package com.pnoker.rtmp.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 指令任务实体类，用于存放队列信息
 */
public class CommandTask {
    public static Map<String, Tasker> taskMap = new HashMap<>(10);
    public static LinkedBlockingQueue<Tasker> taskQueue = new LinkedBlockingQueue(20);
}
