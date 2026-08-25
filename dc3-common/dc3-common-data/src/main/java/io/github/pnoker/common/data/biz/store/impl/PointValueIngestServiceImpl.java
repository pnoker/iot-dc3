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

import io.github.pnoker.common.data.biz.store.IngestIdempotencyWindow;
import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.mapper.PointValueMapper;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private final PointValueMapper pointValueMapper;
    private final PointValueBuilder pointValueBuilder;
    private final PointValueSampleConverter converter;
    private final DeviceFacade deviceFacade;
    private final TsdbStore tsdbStore;
    private final IngestIdempotencyWindow idempotencyWindow;

    @Override
    public boolean saveValue(PointValueBO valueBO) {
        return !saveValues(List.of(valueBO)).isEmpty();
    }

    @Override
    public List<PointValueBO> saveValues(List<PointValueBO> valueBOList) {
        if (Objects.isNull(valueBOList) || valueBOList.isEmpty()) {
            return List.of();
        }

        List<PointValueBO> candidates = filterLeaseValid(valueBOList);
        if (candidates.isEmpty()) {
            return List.of();
        }

        // One entry per message id: drop within-batch duplicates (first wins)
        // and anything the idempotency window already holds. Marking happens
        // only after the whole batch persisted, so a mid-batch crash replays
        // both writes instead of silently losing the latest projection.
        Map<String, PointValueBO> accepted = new LinkedHashMap<>();
        for (PointValueBO value : candidates) {
            if (Objects.isNull(value.getMessageId())) {
                log.warn("Dropping point value without message id: tenantId={}, deviceId={}, pointId={}",
                        value.getTenantId(), value.getDeviceId(), value.getPointId());
                continue;
            }
            if (accepted.containsKey(value.getMessageId()) || idempotencyWindow.seen(value.getMessageId())) {
                continue;
            }
            accepted.put(value.getMessageId(), value);
        }
        if (accepted.isEmpty()) {
            return List.of();
        }

        // One entry per natural key (tenant/device/point/createTime): a batch
        // holding two samples with the same natural key but different message
        // ids would poison stores whose batch upsert cannot affect the same
        // row twice (timescale ON CONFLICT). Last occurrence wins — the same
        // last-write-wins rule the stores apply on replay.
        Map<List<Object>, PointValueBO> byNaturalKey = new LinkedHashMap<>();
        for (PointValueBO value : accepted.values()) {
            byNaturalKey.put(Arrays.asList(value.getTenantId(), value.getDeviceId(),
                    value.getPointId(), value.getCreateTime()), value);
        }
        List<PointValueBO> acceptedValues = new ArrayList<>(byNaturalKey.values());
        // Persistence runs in INGEST_ORDER; the caller gets its own input order.
        List<PointValueBO> ordered = new ArrayList<>(acceptedValues);
        ordered.sort(INGEST_ORDER);

        tsdbStore.append(converter.toSamples(ordered));
        pointValueMapper.upsertLatestBatch(pointValueBuilder.buildDOListByBOList(ordered));
        acceptedValues.forEach(value -> idempotencyWindow.mark(value.getMessageId()));
        return acceptedValues;
    }

    /**
     * Resolve the active lease owner once per distinct device and keep only
     * the values whose send envelope matches it.
     */
    private List<PointValueBO> filterLeaseValid(List<PointValueBO> values) {
        Map<Long, FacadeDeviceOwnerBO> ownersByDevice = new HashMap<>();
        List<PointValueBO> valid = new ArrayList<>(values.size());
        for (PointValueBO value : values) {
            if (Objects.isNull(value.getTenantId()) || Objects.isNull(value.getDeviceId())
                    || Objects.isNull(value.getPointId())) {
                log.warn("Dropping point value with incomplete series key: {}", value);
                continue;
            }
            FacadeDeviceOwnerBO owner = ownersByDevice.computeIfAbsent(value.getDeviceId(),
                    deviceId -> deviceFacade.getActiveOwner(value.getTenantId(), deviceId));
            if (isCurrentOwner(owner, value)) {
                valid.add(value);
            } else {
                log.debug("Dropping stale-owner point value: tenantId={}, deviceId={}, pointId={}, messageId={}",
                        value.getTenantId(), value.getDeviceId(), value.getPointId(), value.getMessageId());
            }
        }
        return valid;
    }

    private boolean isCurrentOwner(FacadeDeviceOwnerBO owner, PointValueBO value) {
        return Objects.nonNull(owner)
                && Objects.equals(owner.driverId(), value.getDriverId())
                && Objects.equals(owner.ownerNode(), value.getDriverNode())
                && Objects.equals(owner.fencingToken(), value.getFencingToken());
    }

}
