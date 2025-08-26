/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
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