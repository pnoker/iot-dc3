package com.pnoker.common.sdk.service;

/**
 * @author pnoker
 */
public interface DriverService {
    /**
     * 读操作
     *
     * @param deviceId
     * @param pointId
     */
    void read(Long deviceId, Long pointId);
}
