package com.github.pnoker.common.sdk.service.job;

import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.service.pool.ThreadPool;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverCommandService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 采集调度任务
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverReadScheduleJob extends QuartzJobBean {
    @Resource
    private ThreadPool threadPool;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverCommandService driverCommandService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap = driverContext.getDevicePointInfoMap();
        for (Long deviceId : pointInfoMap.keySet()) {
            for (Long pointId : pointInfoMap.get(deviceId).keySet()) {
                threadPool.execute(() -> {
                    log.debug("execute read schedule for device({}),point({}),{}", deviceId, pointId, pointInfoMap.get(deviceId).get(pointId));
                    driverCommandService.read(deviceId, pointId);
                });
            }
        }
    }
}