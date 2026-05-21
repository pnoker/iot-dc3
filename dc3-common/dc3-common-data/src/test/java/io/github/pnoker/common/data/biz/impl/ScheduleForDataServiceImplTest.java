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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.data.job.HourlyJobForData;
import io.github.pnoker.common.data.job.PointValueJob;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.quartz.QuartzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.DateBuilder;
import org.quartz.SchedulerException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleForDataServiceImplTest {

    @Mock
    private QuartzService quartzService;

    private PointBatchProperties properties;
    private ScheduleForDataServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new PointBatchProperties();
        properties.setInterval(5);
        service = new ScheduleForDataServiceImpl(properties, quartzService);
    }

    @Test
    void initialRegistersIntervalAndCronJobsAndStartsScheduler() throws Exception {
        service.initial();

        verify(quartzService).createJobWithInterval(
                eq(ScheduleConstant.DATA_SCHEDULE_GROUP),
                eq("data-point-value-schedule-job"),
                eq(5),
                eq(DateBuilder.IntervalUnit.SECOND),
                eq(PointValueJob.class));
        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DATA_SCHEDULE_GROUP),
                eq("hourly-job"),
                eq("0 0 0/1 * * ?"),
                eq(HourlyJobForData.class));
        verify(quartzService).startScheduler();
    }

    @Test
    void initialThrowsServiceExceptionOnSchedulerFailure() throws Exception {
        doThrow(new SchedulerException("scheduler down")).when(quartzService)
                .createJobWithInterval(any(), any(), any(int.class), any(), any());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.initial())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to initialize data scheduler")
                .hasCauseInstanceOf(SchedulerException.class);
        verify(quartzService, never()).startScheduler();
    }
}
