package com.dc3.center.data.service.job;

import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.driver.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author pnoker
 */
@Slf4j
@Component
public class PointValueScheduleJob extends QuartzJobBean {

    @Resource
    private PointValueService pointValueService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    public static List<PointValue> pointValues = new LinkedList<>();
    public static ReentrantReadWriteLock valueLock = new ReentrantReadWriteLock();
    public static AtomicLong valueCount = new AtomicLong(0), valueSpeed = new AtomicLong(0);

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // Statistical point value receive rate
        long speed = valueCount.getAndSet(0);
        valueSpeed.set(speed);
        if (speed > 0) {
            log.debug("Point value receiver speed: {} /s", speed);
        }

        // Save point value array to Redis & MongoDB
        threadPoolExecutor.execute(() -> {
            valueLock.writeLock().lock();
            if (pointValues.size() > 0) {
                pointValueService.addPointValues(pointValues);
                pointValues = new LinkedList<>();
            }
            valueLock.writeLock().unlock();
        });
    }
}
