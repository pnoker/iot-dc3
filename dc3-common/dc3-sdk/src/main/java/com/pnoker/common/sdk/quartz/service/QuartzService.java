package com.pnoker.common.sdk.quartz.service;

import com.pnoker.common.sdk.bean.ScheduleProperty;

/**
 * @author pnoker
 */
public interface QuartzService {
    /**
     * 初始化调度任务
     *
     * @param scheduleProperty
     */
    void initial(ScheduleProperty scheduleProperty);
}
