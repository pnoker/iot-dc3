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
import io.github.pnoker.common.data.job.HourlyJobForData;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.quartz.QuartzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Point-value ingestion no longer has a Quartz tick (RabbitMQ consumer batches drive it),
 * so only the hourly cron job registration is asserted here.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@ExtendWith(MockitoExtension.class)
class ScheduleForDataServiceImplTest {

    @Mock
    private QuartzService quartzService;

    private ScheduleForDataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleForDataServiceImpl(quartzService);
    }

    @Test
    void initialRegistersHourlyJobAndStartsScheduler() throws Exception {
        service.initial();

        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DATA_SCHEDULE_GROUP),
                eq("hourly-job"),
                eq("0 0 0/1 * * ?"),
                eq(HourlyJobForData.class));
        verify(quartzService).createJobWithInterval(
                eq(ScheduleConstant.DATA_SCHEDULE_GROUP),
                eq("point-value-ingest-replay"),
                eq(5),
                eq(org.quartz.DateBuilder.IntervalUnit.SECOND),
                eq(io.github.pnoker.common.data.job.PointValueIngestReplayJob.class));
        verify(quartzService).startScheduler();
    }

    @Test
    void initialThrowsServiceExceptionOnSchedulerFailure() throws Exception {
        doThrow(new SchedulerException("scheduler down")).when(quartzService)
                .createJobWithCron(any(), any(), any(), any());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.initial())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to initialize data scheduler")
                .hasCauseInstanceOf(SchedulerException.class);
        verify(quartzService, never()).startScheduler();
    }
}
