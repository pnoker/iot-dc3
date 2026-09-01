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

package io.github.pnoker.common.data.biz.store.impl;

import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.data.repository.ReactivePointValueIngestOutbox;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Default ingest orchestration (docs/design/tsdb-abstraction.md §9.1/§9.2).
 * The lease guard previously lived inside the history INSERT as a cross-schema
 * join; it is an application concern now — the owner is resolved through the
 * device facade per distinct device in the batch and a value is accepted only
 * when its (driverId, driverNode, fencingToken) envelope matches the active
 * owner. Unlike the SQL join this check is not taken under FOR KEY SHARE, so
 * a failover racing the append can slip one stale history row through; the
 * fenced latest projection stays correct either way.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointValueIngestServiceImpl implements PointValueIngestService {

    private static final Comparator<PointValueBO> INGEST_ORDER = Comparator
            .comparing(PointValueBO::getTenantId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getDeviceId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getPointId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getFencingToken, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getSequence, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(PointValueBO::getMessageId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final ReactivePointValueLatestStore latestStore;
    private final PointValueBuilder pointValueBuilder;
    private final PointValueSampleConverter converter;
    private final DeviceFacade deviceFacade;
    private final ReactiveTsdbStore reactiveTsdbStore;
    private final ReactivePointValueIngestOutbox ingestOutbox;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    @Override
    public Mono<Boolean> saveValue(PointValueBO valueBO) {
        return saveValues(List.of(valueBO)).map(values -> !values.isEmpty());
    }

    @Override
    public Mono<List<PointValueBO>> saveValues(List<PointValueBO> valueBOList) {
        if (Objects.isNull(valueBOList) || valueBOList.isEmpty()) return Mono.just(List.of());
        return filterLeaseValidReactive(valueBOList)
                .map(this::prepare)
                .flatMap(accepted -> {
                    if (accepted.isEmpty()) return Mono.just(List.of());
                    List<PointValueBO> ordered = new ArrayList<>(accepted);
                    ordered.sort(INGEST_ORDER);
                    List<io.github.pnoker.common.data.entity.model.PointValueDO> rows =
                            pointValueBuilder.buildDOListByBOList(ordered);
                    String owner = UUID.randomUUID().toString();
                    return enqueue(rows, owner).flatMap(enqueuedRows -> {
                                if (enqueuedRows.isEmpty()) {
                                    Flux<io.github.pnoker.common.data.entity.model.PointValueDO> persisted = ingestOutbox.findPersisted(rows);
                                    return persisted.map(row -> row.getTenantId() + ":" + row.getMessageId()).collectList()
                                            .map(ids -> ordered.stream().filter(row -> ids.contains(row.getTenantId() + ":" + row.getMessageId())).toList());
                                }
                                Map<String, io.github.pnoker.common.data.entity.model.PointValueDO> enqueuedById =
                                        enqueuedRows.stream().collect(java.util.stream.Collectors.toMap(
                                                row -> row.getTenantId() + ":" + row.getMessageId(), row -> row));
                                List<PointValueBO> durableAccepted = ordered.stream()
                                        .filter(row -> enqueuedById.containsKey(row.getTenantId() + ":" + row.getMessageId()))
                                        .toList();
                                if (durableAccepted.isEmpty()) return Mono.just(List.of());
                                List<PointValueBO> acceptedInInputOrder = accepted.stream()
                                        .filter(row -> enqueuedById.containsKey(row.getTenantId() + ":" + row.getMessageId()))
                                        .toList();
                                return reactiveTsdbStore.append(converter.toSamples(durableAccepted))
                                        .then(Mono.defer(() -> latestStore.upsertBatch(enqueuedRows)))
                                        .then(markDurablePersisted(enqueuedRows, owner))
                                        .retryWhen(Retry.backoff(3, java.time.Duration.ofMillis(100))
                                                .maxBackoff(java.time.Duration.ofSeconds(2)))
                                        .thenReturn(acceptedInInputOrder);
                            });
                });
    }

    private List<PointValueBO> prepare(List<PointValueBO> candidates) {
        if (candidates.isEmpty()) return List.of();
        Map<String, PointValueBO> accepted = new LinkedHashMap<>();
        for (PointValueBO value : candidates) {
            if (Objects.isNull(value.getMessageId())) {
                log.warn("Dropping point value without message id: tenantId={}, deviceId={}, pointId={}",
                        value.getTenantId(), value.getDeviceId(), value.getPointId());
                continue;
            }
            String dedupKey = value.getTenantId() + ":" + value.getMessageId();
            if (accepted.containsKey(dedupKey)) continue;
            accepted.put(dedupKey, value);
        }
        if (accepted.isEmpty()) return List.of();
        Map<List<Object>, PointValueBO> byNaturalKey = new LinkedHashMap<>();
        for (PointValueBO value : accepted.values()) {
            byNaturalKey.put(Arrays.asList(value.getTenantId(), value.getDeviceId(),
                    value.getPointId(), value.getCreateTime()), value);
        }
        return new ArrayList<>(byNaturalKey.values());
    }

    @Override
    public Mono<Void> markProcessed(List<PointValueBO> valueBOList) {
        if (valueBOList == null || valueBOList.isEmpty()) return Mono.empty();
        return Flux.fromIterable(valueBOList)
                .filter(Objects::nonNull)
                .map(pointValueBuilder::buildDOByBO)
                .concatMap(row -> {
                    Mono<Integer> result = ingestOutbox.markProcessed(row);
                    return result.switchIfEmpty(Mono.just(0)).flatMap(updated -> updated == 1
                            ? Mono.just(updated)
                            : Mono.error(new IllegalStateException("Ingest receipt is not PERSISTED: "
                                    + row.getTenantId() + ":" + row.getMessageId())));
                }).then();
    }

    private Mono<List<io.github.pnoker.common.data.entity.model.PointValueDO>> enqueue(
            List<io.github.pnoker.common.data.entity.model.PointValueDO> rows, String owner) {
        return ingestOutbox.enqueue(rows, owner);
    }

    private Mono<Void> markDurablePersisted(List<io.github.pnoker.common.data.entity.model.PointValueDO> rows, String owner) {
        if (rows == null || rows.isEmpty()) return Mono.empty();
        return Flux.fromIterable(rows).concatMap(row -> {
            Mono<Integer> result = ingestOutbox.markPersisted(row, owner);
            return result.switchIfEmpty(Mono.just(0)).flatMap(updated -> updated == 1
                    ? Mono.just(updated)
                    : Mono.error(new IllegalStateException("Ingest receipt claim was lost: "
                            + row.getTenantId() + ":" + row.getMessageId())));
        }).then();
    }

    @Override
    public Mono<Integer> replayPending() {
        String owner = UUID.randomUUID().toString();
        return ingestOutbox.claim(owner, 100)
                .concatMap(row -> replayOne(row, owner).onErrorResume(error -> ingestOutbox.markFailed(row, owner,
                                error.getMessage()).flatMap(updated -> updated == 1 ? Mono.just(0)
                                        : Mono.error(new IllegalStateException("Ingest receipt claim was lost while failing: "
                                                + row.getTenantId() + ":" + row.getMessageId(), error)))))
                .reduce(0, Integer::sum);
    }

    private Mono<Integer> replayOne(io.github.pnoker.common.data.entity.model.PointValueDO row, String owner) {
        PointValueBO value = pointValueBuilder.buildBOByDO(row);
        Mono<Integer> latest = latestStore.upsertBatch(List.of(row));
        return reactiveTsdbStore.append(converter.toSamples(List.of(value)))
                .then(latest)
                .then(alarmRuleTriggerService.processPointValue(value))
                .then(ingestOutbox.markPersisted(row, owner))
                .flatMap(updated -> updated == 1 ? ingestOutbox.markProcessed(row)
                        : Mono.error(new IllegalStateException("Ingest receipt claim was lost: "
                                + row.getTenantId() + ":" + row.getMessageId())))
                .flatMap(updated -> updated == 1 ? Mono.just(updated)
                        : Mono.error(new IllegalStateException("Ingest receipt was not marked processed: "
                                + row.getTenantId() + ":" + row.getMessageId())))
                .thenReturn(1);
    }

    /**
     * Resolve the active lease owner once per distinct device and keep only
     * the values whose send envelope matches it.
     */
    private Mono<List<PointValueBO>> filterLeaseValidReactive(List<PointValueBO> values) {
        Map<String, Mono<FacadeDeviceOwnerBO>> owners = new HashMap<>();
        return reactor.core.publisher.Flux.fromIterable(values)
                .filter(value -> {
                    boolean valid = value != null && value.getTenantId() != null && value.getDeviceId() != null
                            && value.getPointId() != null;
                    if (!valid) log.warn("Dropping point value with incomplete series key: {}", value);
                    return valid;
                })
                .concatMap(value -> {
                    String key = value.getTenantId() + ":" + value.getDeviceId();
                    Mono<FacadeDeviceOwnerBO> owner = owners.computeIfAbsent(key,
                            ignored -> safe(deviceFacade.getActiveOwnerReactive(value.getTenantId(), value.getDeviceId())).cache());
                    return owner.filter(candidate -> isCurrentOwner(candidate, value)).map(ignored -> value)
                            .switchIfEmpty(Mono.fromRunnable(() -> log.debug(
                                    "Dropping stale-owner point value: tenantId={}, deviceId={}, pointId={}, messageId={}",
                                    value.getTenantId(), value.getDeviceId(), value.getPointId(), value.getMessageId())))
                            .flux();
                }).collectList();
    }

    private Mono<FacadeDeviceOwnerBO> safe(Mono<FacadeDeviceOwnerBO> publisher) {
        return publisher == null ? Mono.empty() : publisher;
    }

    private boolean isCurrentOwner(FacadeDeviceOwnerBO owner, PointValueBO value) {
        return Objects.nonNull(owner)
                && Objects.equals(owner.driverId(), value.getDriverId())
                && Objects.equals(owner.ownerNode(), value.getDriverNode())
                && Objects.equals(owner.fencingToken(), value.getFencingToken());
    }

}
