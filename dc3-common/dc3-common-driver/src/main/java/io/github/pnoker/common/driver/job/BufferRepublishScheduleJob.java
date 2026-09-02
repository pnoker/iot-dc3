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
package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.buffer.BufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Quartz job that drains the local point-value buffer back to RabbitMQ. Failures are
 * logged (not rethrown) so one bad batch does not abort the scheduler.
 *
 * @author pnoker
 * @since 2026.6.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class BufferRepublishScheduleJob extends QuartzJobBean {

    private final BufferService bufferService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        try {
            bufferService.republishBatch();
        } catch (Exception e) {
            log.error("Buffer republish job failed", e);
        }
    }
}
