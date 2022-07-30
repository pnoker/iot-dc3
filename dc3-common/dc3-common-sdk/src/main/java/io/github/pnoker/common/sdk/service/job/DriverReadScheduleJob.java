/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.common.sdk.service.job;

import io.github.pnoker.common.bean.driver.AttributeInfo;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverCommandService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Read Schedule Job
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverReadScheduleJob extends QuartzJobBean {

    @Resource
    private DriverContext driverContext;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private DriverCommandService driverCommandService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Map<String, Device> deviceMap = driverContext.getDriverMetadata().getDeviceMap();
        deviceMap.values().forEach(device -> {
            Set<String> profileIds = device.getProfileIds();
            Map<String, Map<String, AttributeInfo>> pointInfoMap = driverContext.getDriverMetadata().getPointInfoMap().get(device.getId());
            if (null != pointInfoMap && null != profileIds) {
                profileIds.forEach(profileId -> {
                    Map<String, Point> pointMap = driverContext.getDriverMetadata().getProfilePointMap().get(profileId);
                    if (null != pointMap) {
                        pointMap.keySet().forEach(pointId -> {
                            Map<String, AttributeInfo> map = pointInfoMap.get(pointId);
                            if (null != map) {
                                threadPoolExecutor.execute(() -> driverCommandService.read(device.getId(), pointId));
                            }
                        });
                    }
                });
            }
        });
    }
}