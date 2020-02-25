package com.pnoker.common.sdk.service;

import com.pnoker.common.sdk.bean.ScheduleProperty;

/**
 * @author pnoker
 */
public interface DriverScheduleService {
    /**
     * 初始化调度任务
     *
     * @param scheduleProperty
     */
    void initial(ScheduleProperty scheduleProperty);
}
