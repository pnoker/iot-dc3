package io.github.pnoker.center.data.job;


import io.github.pnoker.center.data.biz.DeviceStatisticsOnlineService;
import io.github.pnoker.center.data.biz.DriverStatisticsOnlineService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class DeviceStatisticsOnlineJob extends QuartzJobBean {

    @Resource
    private DeviceStatisticsOnlineService deviceStatisticsOnlineService;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        deviceStatisticsOnlineService.deviceStatisticsOnline();

    }
}
