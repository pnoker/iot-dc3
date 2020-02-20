package com.pnoker.common.sdk.quartz.job;

import com.pnoker.common.sdk.service.DriverCustomizersService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 采集调度任务
 *
 * @author pnoker
 */
@Slf4j
@Component
public class ReadJob extends QuartzJobBean {
    @Resource
    private DriverCustomizersService customizersService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("read job");
    }
}