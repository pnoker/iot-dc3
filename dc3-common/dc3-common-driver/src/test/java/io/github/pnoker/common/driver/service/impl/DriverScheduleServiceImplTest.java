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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.job.DriverCustomScheduleJob;
import io.github.pnoker.common.driver.job.DriverReadScheduleJob;
import io.github.pnoker.common.driver.job.DriverStatusScheduleJob;
import io.github.pnoker.common.driver.job.DeviceHealthScheduleJob;
import io.github.pnoker.common.exception.CronException;
import io.github.pnoker.common.quartz.QuartzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DriverScheduleServiceImplTest {

    @Mock
    private QuartzService quartzService;

    private DriverProperties properties;
    private DriverScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new DriverProperties();
        service = new DriverScheduleServiceImpl(properties, quartzService);
    }

    @Test
    void initialNoOpsWhenScheduleConfigMissing() {
        properties.setSchedule(null);
        assertThatNoException().isThrownBy(() -> service.initial());
        verifyNoInteractions(quartzService);
    }

    @Test
    void initialAlwaysRegistersStatusJob() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        properties.setSchedule(s);
        service.initial();
        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DRIVER_SCHEDULE_GROUP),
                eq(ScheduleConstant.DRIVER_STATUS_SCHEDULE_JOB),
                eq(ScheduleConstant.DRIVER_STATUS_SCHEDULE_CRON),
                eq(DriverStatusScheduleJob.class));
        verify(quartzService).startScheduler();
    }

    @Test
    void initialRegistersHealthJobWhenEnabled() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        properties.setSchedule(s);
        properties.getHealth().getDevice().setCron("0/20 * * * * ?");
        service.initial();
        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DRIVER_SCHEDULE_GROUP),
                eq(ScheduleConstant.DEVICE_HEALTH_SCHEDULE_JOB),
                eq("0/20 * * * * ?"),
                eq(DeviceHealthScheduleJob.class));
    }

    @Test
    void initialSkipsHealthJobWhenDisabled() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        properties.setSchedule(s);
        properties.getHealth().getDevice().setEnable(false);
        service.initial();
        verify(quartzService, never()).createJobWithCron(
                eq(ScheduleConstant.DRIVER_SCHEDULE_GROUP),
                eq(ScheduleConstant.DEVICE_HEALTH_SCHEDULE_JOB),
                any(),
                eq(DeviceHealthScheduleJob.class));
    }

    @Test
    void initialRejectsInvalidHealthCron() {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        properties.setSchedule(s);
        properties.getHealth().getDevice().setCron("nope");
        assertThatThrownBy(() -> service.initial())
                .isInstanceOf(CronException.class)
                .hasMessageContaining("Device health schedule");
    }

    @Test
    void initialRegistersReadJobWhenEnabled() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        s.getRead().setEnable(true);
        s.getRead().setCron("0 */1 * * * ?");
        properties.setSchedule(s);
        service.initial();
        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DRIVER_SCHEDULE_GROUP),
                eq(ScheduleConstant.DRIVER_READ_SCHEDULE_JOB),
                eq("0 */1 * * * ?"),
                eq(DriverReadScheduleJob.class));
    }

    @Test
    void initialRejectsInvalidReadCron() {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        s.getRead().setEnable(true);
        s.getRead().setCron("definitely-not-a-cron");
        properties.setSchedule(s);
        assertThatThrownBy(() -> service.initial())
                .isInstanceOf(CronException.class)
                .hasMessageContaining("Read schedule");
    }

    @Test
    void initialRegistersCustomJobWhenEnabled() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        s.getCustom().setEnable(true);
        s.getCustom().setCron("0 0/5 * * * ?");
        properties.setSchedule(s);
        service.initial();
        verify(quartzService).createJobWithCron(
                eq(ScheduleConstant.DRIVER_SCHEDULE_GROUP),
                eq(ScheduleConstant.DRIVER_CUSTOM_SCHEDULE_JOB),
                eq("0 0/5 * * * ?"),
                eq(DriverCustomScheduleJob.class));
    }

    @Test
    void initialRejectsInvalidCustomCron() {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        s.getCustom().setEnable(true);
        s.getCustom().setCron("garbage");
        properties.setSchedule(s);
        assertThatThrownBy(() -> service.initial())
                .isInstanceOf(CronException.class)
                .hasMessageContaining("Custom schedule");
    }

    @Test
    void initialWrapsSchedulerExceptionInServiceException() throws Exception {
        DriverProperties.ScheduleProperties s = new DriverProperties.ScheduleProperties();
        properties.setSchedule(s);
        doThrow(new SchedulerException("scheduler down")).when(quartzService)
                .createJobWithCron(any(), any(), any(), any());
        assertThatThrownBy(() -> service.initial())
                .isInstanceOf(io.github.pnoker.common.exception.ServiceException.class)
                .hasMessageContaining("Failed to initialize driver scheduler");
        verify(quartzService, never()).startScheduler();
    }
}
