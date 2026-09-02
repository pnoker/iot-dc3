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
package io.github.pnoker.common.data.job;

import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/** Replays durable point-value receipts left by a crashed Data Center instance. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueIngestReplayJob extends QuartzJobBean {

    private final PointValueIngestService ingestService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        ingestService
                .replayPending()
                .subscribe(
                        count -> {
                            if (count > 0) log.info("Replayed {} point-value ingest receipts", count);
                        },
                        error -> log.error("Point-value ingest replay failed", error));
    }
}
