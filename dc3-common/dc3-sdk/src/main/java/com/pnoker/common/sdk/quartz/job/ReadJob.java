package com.pnoker.common.sdk.quartz.job;

import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.DriverService;
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
public class ReadJob extends QuartzJobBean {
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private DriverService driverService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap = deviceDriver.getPointInfoMap();
        for (Long deviceId : pointInfoMap.keySet()) {
            for (Long pointId : pointInfoMap.get(deviceId).keySet()) {
                log.debug("execute read schedule for device({}),point({})", deviceId, pointId);
                driverService.read(deviceId, pointId);
            }
        }
    }
}