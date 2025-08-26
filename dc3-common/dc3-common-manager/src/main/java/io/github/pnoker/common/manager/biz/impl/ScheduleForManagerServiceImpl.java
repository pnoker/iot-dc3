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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.manager.biz.ScheduleForManagerService;
import io.github.pnoker.common.manager.job.HourlyJobForManager;
import io.github.pnoker.common.quartz.QuartzService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ScheduleForManagerServiceImpl implements ScheduleForManagerService {

    @Resource
    private QuartzService quartzService;

    @Override
    public void initial() {
        try {
            // 自定义调度
            quartzService.createJobWithCron(ScheduleConstant.MANAGER_SCHEDULE_GROUP, "hourly-job", "0 0 0/1 * * ?", HourlyJobForManager.class);

            quartzService.startScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
