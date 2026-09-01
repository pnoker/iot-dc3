package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.dashboard.AlertCountersRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.HourCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.ActivityCellRow;
import io.github.pnoker.common.data.entity.bo.dashboard.FlappingRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.CorrelationPairRow;
import io.github.pnoker.common.data.entity.bo.dashboard.PeerAlarmRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AgingBucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.MttaTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.ProtocolHealthRow;
import io.github.pnoker.common.data.entity.bo.dashboard.RecentChangeRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceStatsRow;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Reactive, tenant-scoped aggregate reads for dashboard alerts. */
public interface ReactiveAlertAnalyticsStore {

    Mono<AlertCountersRow> countAll(long tenantId);

    Flux<BucketRow> countByType(long tenantId);

    Flux<SourceStatsRow> countBySource(long tenantId);

    Flux<HourCountRow> hourlyCounts(long tenantId, LocalDateTime from);

    Flux<SourceStatsRow> todayBySource(long tenantId, LocalDateTime from);

    Flux<AlertTrendRow> dailyTrend(long tenantId, LocalDateTime from);

    Flux<SourceCountRow> topSources(long tenantId, LocalDateTime from, int limit);

    Flux<ActivityCellRow> activityHeatmap(long tenantId, LocalDateTime from);

    Flux<BucketRow> typeDistribution(long tenantId, LocalDateTime from);

    Flux<SourceCountRow> stormSources(long tenantId, LocalDateTime from, int minCount, int limit);

    Flux<FlappingRow> flappingSources(long tenantId, LocalDateTime from, int minCount, int limit);

    Flux<CorrelationPairRow> correlationPairs(long tenantId, LocalDateTime from, int windowSec, int limit);

    Flux<PeerAlarmRow> peerAlarmCounts(long tenantId, LocalDateTime from);

    Mono<AgingBucketRow> agingBuckets(long tenantId);

    Flux<MttaTrendRow> mttaByDay(long tenantId, LocalDateTime from);

    Flux<ProtocolHealthRow> protocolHealth(long tenantId);

    Flux<RecentChangeRow> recentChanges(long tenantId, LocalDateTime from, int limit);
}
