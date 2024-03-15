package io.github.pnoker.center.data.job;

import io.github.pnoker.center.data.biz.DeviceOnlineJobService;
import io.github.pnoker.center.data.biz.DriverOnlineJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 统计驱动在线时长
 *
 * @author zcx
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceOnlineJob extends QuartzJobBean {
    /**
     * 任务执行
     * * <p>
     * * 具体逻辑请在 biz service 中定义
     *
     * @param context JobExecutionContext
     * @throws JobExecutionException JobExecutionException
     */


    @Resource
    private DeviceOnlineJobService deviceOnlineJobService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        deviceOnlineJobService.deviceOnline();
        log.info("设备状态统计---------");
    }
}
