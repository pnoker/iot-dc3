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

package io.github.pnoker.common.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronTrigger;
import org.quartz.DateBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuartzServiceTest {

    @Mock
    private Scheduler scheduler;

    private QuartzService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new QuartzService();
        Field field = QuartzService.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        field.set(service, scheduler);
    }

    @Test
    void createJobWithIntervalRegistersDetailAndSimpleTrigger() throws SchedulerException {
        service.createJobWithInterval("group-1", "job-1", 5, DateBuilder.IntervalUnit.SECOND, SampleJob.class);

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());

        JobDetail detail = jobCaptor.getValue();
        assertThat(detail.getKey().getGroup()).isEqualTo("group-1");
        assertThat(detail.getKey().getName()).isEqualTo("job-1");
        assertThat(detail.getJobClass()).isEqualTo(SampleJob.class);

        Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger).isInstanceOf(SimpleTrigger.class);
        SimpleTrigger simple = (SimpleTrigger) trigger;
        assertThat(simple.getRepeatInterval()).isEqualTo(5_000L);
        assertThat(simple.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);
    }

    @Test
    void createJobWithCronRegistersDetailAndCronTrigger() throws SchedulerException {
        service.createJobWithCron("group-2", "job-2", "0 0/5 * * * ?", SampleJob.class);

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(org.mockito.ArgumentMatchers.any(JobDetail.class), triggerCaptor.capture());

        Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger).isInstanceOf(CronTrigger.class);
        assertThat(((CronTrigger) trigger).getCronExpression()).isEqualTo("0 0/5 * * * ?");
        assertThat(trigger.getKey().getGroup()).isEqualTo("group-2");
        assertThat(trigger.getKey().getName()).isEqualTo("job-2");
    }

    @Test
    void createJobWithCronRejectsInvalidExpression() {
        assertThatThrownBy(() -> service.createJobWithCron("g", "n", "garbage", SampleJob.class))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void startSchedulerNoOpsWhenAlreadyShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        service.startScheduler();
        verify(scheduler, never()).start();
    }

    @Test
    void startSchedulerStartsWhenLive() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        service.startScheduler();
        verify(scheduler).start();
    }

    @Test
    void stopSchedulerNoOpsWhenAlreadyShutdown() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(true);
        service.stopScheduler();
        verify(scheduler, never()).shutdown();
    }

    @Test
    void stopSchedulerShutsDownLiveScheduler() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        service.stopScheduler();
        verify(scheduler).shutdown();
    }

    @Test
    void startSchedulerPropagatesFailure() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        org.mockito.Mockito.doThrow(new SchedulerException("boom")).when(scheduler).start();
        assertThatThrownBy(() -> service.startScheduler()).isInstanceOf(SchedulerException.class);
    }

    public static class SampleJob extends QuartzJobBean {
        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            // no-op for testing
        }
    }
}
