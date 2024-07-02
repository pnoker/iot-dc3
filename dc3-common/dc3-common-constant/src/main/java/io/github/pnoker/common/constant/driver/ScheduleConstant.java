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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 任务调度 相关常量
 *
 * @author pnoker
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
