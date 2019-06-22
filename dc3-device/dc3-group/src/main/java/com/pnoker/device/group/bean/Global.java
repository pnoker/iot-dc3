package com.pnoker.device.group.bean;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
public class Global {
    public static LinkedBlockingQueue<Thread> wiaReceiveThreadQueue = new LinkedBlockingQueue(20);
}
