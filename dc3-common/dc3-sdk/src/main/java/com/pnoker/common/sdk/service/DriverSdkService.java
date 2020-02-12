package com.pnoker.common.sdk.service;

import com.pnoker.common.sdk.init.DeviceDriver;
import org.springframework.context.ApplicationContext;

/**
 * @author pnoker
 */
public interface DriverSdkService {
    /**
     * 初始化 SDK
     *
     * @param context
     * @param deviceDriver 驱动
     * @return
     */
    boolean initial(ApplicationContext context, DeviceDriver deviceDriver);

    /**
     * 驱动注册
     *
     * @param deviceDriver 驱动
     * @return
     */
    boolean register(DeviceDriver deviceDriver);
}
