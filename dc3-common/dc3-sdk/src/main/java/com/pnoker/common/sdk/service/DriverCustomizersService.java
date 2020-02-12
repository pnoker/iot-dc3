package com.pnoker.common.sdk.service;

import com.pnoker.common.sdk.init.DeviceDriver;

/**
 * 设备驱动接口，用于设备驱动接口实现
 *
 * @author pnoker
 */
public interface DriverCustomizersService {
    /**
     * 初始化设备驱动
     *
     * @param deviceDriver
     */
    void initial(DeviceDriver deviceDriver);

    /**
     * 驱动本身存在定时器，用于定时采集数据和下发数据，该方法为用户自定义操作，系统根据配置定时执行
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
     * 提供http接口模式接收数据
     */
    void receive();

    /**
     * 设备状态
     */
    void status();
}
