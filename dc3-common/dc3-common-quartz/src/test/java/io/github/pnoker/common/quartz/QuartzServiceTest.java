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
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuartzServiceTest {

    @Mock
    private Scheduler scheduler;

    private QuartzService service;

    @BeforeEach
    void setUp() {
        service = new QuartzService(scheduler);
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
    void createJobWithIntervalHonorsIntervalUnit() throws SchedulerException {
        service.createJobWithInterval("group-1", "job-1", 2, DateBuilder.IntervalUnit.MINUTE, SampleJob.class);

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(org.mockito.ArgumentMatchers.any(JobDetail.class), triggerCaptor.capture());

        SimpleTrigger simple = (SimpleTrigger) triggerCaptor.getValue();
        assertThat(simple.getRepeatInterval()).isEqualTo(120_000L);
    }

    @Test
    void createJobWithIntervalRejectsInvalidInputs() {
        assertThatThrownBy(() -> service.createJobWithInterval("g", "n", 0, DateBuilder.IntervalUnit.SECOND,
                SampleJob.class)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interval");
        assertThatThrownBy(() -> service.createJobWithInterval("g", "n", 1, DateBuilder.IntervalUnit.MONTH,
                SampleJob.class)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variable length");
    }

    @Test
    void createJobDeletesExistingJobBeforeScheduling() throws SchedulerException {
        when(scheduler.checkExists(JobKey.jobKey("job-1", "group-1"))).thenReturn(true);

        service.createJobWithInterval("group-1", "job-1", 5, DateBuilder.IntervalUnit.SECOND, SampleJob.class);

        verify(scheduler).deleteJob(JobKey.jobKey("job-1", "group-1"));
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron expression is invalid");
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
        when(scheduler.isStarted()).thenReturn(false);
        service.startScheduler();
        verify(scheduler).start();
    }

    @Test
    void startSchedulerNoOpsWhenAlreadyStarted() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        when(scheduler.isStarted()).thenReturn(true);
        service.startScheduler();
        verify(scheduler, never()).start();
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
    void stopSchedulerCanWaitForJobsToComplete() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        service.stopScheduler(true);
        verify(scheduler).shutdown(true);
    }

    @Test
    void startSchedulerPropagatesFailure() throws SchedulerException {
        when(scheduler.isShutdown()).thenReturn(false);
        when(scheduler.isStarted()).thenReturn(false);
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
