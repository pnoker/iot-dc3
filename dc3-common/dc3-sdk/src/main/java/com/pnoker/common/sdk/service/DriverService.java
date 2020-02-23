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
    String read(Long deviceId, Long pointId);

    /**
     * 写操作
     *
     * @param deviceId
     * @param pointId
     * @param value
     * @return
     */
    Boolean write(Long deviceId, Long pointId, String value);
}
