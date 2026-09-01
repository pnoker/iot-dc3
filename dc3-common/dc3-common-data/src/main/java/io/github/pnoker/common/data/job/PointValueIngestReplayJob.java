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
        ingestService.replayPending().subscribe(
                count -> { if (count > 0) log.info("Replayed {} point-value ingest receipts", count); },
                error -> log.error("Point-value ingest replay failed", error));
    }
}
