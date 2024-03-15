package io.github.pnoker.center.data.job;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcPageDTO;
import io.github.pnoker.center.data.biz.DriverOnlineJobService;
import io.github.pnoker.center.data.dal.DriverStatusHistoryManager;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DriverQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.ObjectUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * 统计驱动在线时长
 *
 * @author zcx
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverOnlineJob extends QuartzJobBean {
    /**
     * 任务执行
     * * <p>
     * * 具体逻辑请在 biz service 中定义
     *
     * @param context JobExecutionContext
     * @throws JobExecutionException JobExecutionException
     */


    @Resource
    private DriverOnlineJobService driverOnlineJobService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        driverOnlineJobService.driverOnline();
        log.info("驱动状态统计---------");
    }
}
