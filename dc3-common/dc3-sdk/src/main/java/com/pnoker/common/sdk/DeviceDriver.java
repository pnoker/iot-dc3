package com.pnoker.common.sdk;

/**
 * 设备驱动接口，用于设备驱动接口实现
 *
 * @author pnoker
 */
public interface DeviceDriver {
    /**
     * 初始化设备驱动
     */
    void initial();

    /**
     * 自定义调度实现
     */
    void schedule();

    /**
     * 读操作
     */
    void read();

    /**
     * 写操作
     */
    void write();

    /**
     * 设备状态
     */
    void status();
}
