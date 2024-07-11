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

package io.github.pnoker.common.driver.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * 读任务
 * <p>
 * 系统内置定时任务
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverReadScheduleJob extends QuartzJobBean {

    @Resource
    private DriverMetadata driverMetadata;
    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private DriverReadService driverReadService;

    @Override
    protected void executeInternal(@NotNull JobExecutionContext jobExecutionContext) {
        Set<Long> deviceIds = driverMetadata.getDeviceIds();
        if (CollUtil.isEmpty(deviceIds)) {
            return;
        }

        for (Long deviceId : deviceIds) {
            DeviceBO entityBO = deviceMetadata.getCache(deviceId);
            if (Objects.nonNull(entityBO)
                    && EnableFlagEnum.ENABLE.equals(entityBO.getEnableFlag())
                    && CollUtil.isNotEmpty(entityBO.getProfileIds())
                    && CollUtil.isNotEmpty(entityBO.getPointIds())
                    && MapUtil.isNotEmpty(entityBO.getDriverAttributeConfigIdMap())
                    && MapUtil.isNotEmpty(entityBO.getPointAttributeConfigIdMap())
            ) {
                Set<Long> pointIds = entityBO.getPointIds();
                for (Long pointId : pointIds) {
                    try {
                        driverReadService.read(deviceId, pointId);
                    } catch (Exception e) {
                        log.error("Read device[{}], point[{}] error: {}", deviceId, pointId, e.getMessage(), e);
                    }
                }
            }
        }
    }
}