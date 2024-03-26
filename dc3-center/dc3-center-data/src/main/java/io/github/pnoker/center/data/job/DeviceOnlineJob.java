/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.job;

import io.github.pnoker.center.data.biz.DeviceOnlineJobService;
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
