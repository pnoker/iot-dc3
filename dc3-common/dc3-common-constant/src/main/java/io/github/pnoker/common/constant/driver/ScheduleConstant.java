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

import io.github.pnoker.common.constant.common.BaseConstant;


/**
 * Constants for scheduled job intervals and cron expressions.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class ScheduleConstant {

    /**
     * Driver schedule group
     */
    public static final String DRIVER_SCHEDULE_GROUP = "driver-schedule-group";

    /**
     * Driver read schedule job
     */
    public static final String DRIVER_READ_SCHEDULE_JOB = "read-schedule-job";

    /**
     * Driver custom schedule job
     */
    public static final String DRIVER_CUSTOM_SCHEDULE_JOB = "customs-chedule-job";

    /**
     * Driver status schedule job
     */
    public static final String DRIVER_STATUS_SCHEDULE_JOB = "status-schedule-job";

    /**
     * Device health schedule job
     */
    public static final String DEVICE_HEALTH_SCHEDULE_JOB = "device-health-schedule-job";

    /**
     * Driver status schedule cron
     */
    public static final String DRIVER_STATUS_SCHEDULE_CRON = "0/15 * * * * ?";

    /**
     * Data schedule group
     */
    public static final String DATA_SCHEDULE_GROUP = "data-schedule-group";

    /**
     * Manager schedule group
     */
    public static final String MANAGER_SCHEDULE_GROUP = "manager-schedule-group";

    private ScheduleConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
