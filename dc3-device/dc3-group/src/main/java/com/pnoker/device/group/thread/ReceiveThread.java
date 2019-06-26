package com.pnoker.device.group.thread;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 数据采集线程基类，数采线程请务必继承此类
 */
public class ReceiveThread implements Runnable {
    /**
     * 线程名称，建议写明白
     */
    private String name;

    public ReceiveThread(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {

    }

    @Override
    public String toString() {
        return "ReceiveThread [name=" + name + "]";
    }
}
