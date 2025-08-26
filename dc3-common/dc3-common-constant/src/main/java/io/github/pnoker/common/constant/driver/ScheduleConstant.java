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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 任务调度 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class ScheduleConstant {

    /**
     * 驱动任务调度分组
     */
    public static final String DRIVER_SCHEDULE_GROUP = "driver-schedule-group";

    /**
     * 驱动读任务
     */
    public static final String DRIVER_READ_SCHEDULE_JOB = "read-schedule-job";

    /**
     * 驱动自定义任务
     */
    public static final String DRIVER_CUSTOM_SCHEDULE_JOB = "customs-chedule-job";

    /**
     * 驱动状态任务
     */
    public static final String DRIVER_STATUS_SCHEDULE_JOB = "status-schedule-job";

    /**
     * 驱动状态任务 Cron
     */
    public static final String DRIVER_STATUS_SCHEDULE_CRON = "0/15 * * * * ?";

    /**
     * 数据任务调度分组
     */
    public static final String DATA_SCHEDULE_GROUP = "data-schedule-group";

    /**
     * 管理任务调度分组
     */
    public static final String MANAGER_SCHEDULE_GROUP = "manager-schedule-group";

    private ScheduleConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
