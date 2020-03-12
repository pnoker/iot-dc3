package com.github.pnoker.common.sdk.service;

import com.github.pnoker.common.sdk.bean.ScheduleProperty;

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
