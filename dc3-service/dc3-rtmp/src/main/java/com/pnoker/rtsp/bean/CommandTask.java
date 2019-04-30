package com.pnoker.rtsp.bean;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 指令任务实体类，用于存放队列信息
 */
public class CommandTask {
    public static LinkedBlockingQueue taskQueue = new LinkedBlockingQueue(20);
}
