package com.pnoker.common.sdk.service;

/**
 * @author pnoker
 */
public interface DriverService {
    /**
     * 驱动注册
     *
     * @param name        驱动名称
     * @param serviceName 驱动服务名称
     * @param description 驱动描述
     * @return
     */
    boolean register(String name, String serviceName, String description);
}
