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

package io.github.pnoker.common.data.biz.store;

import io.github.pnoker.common.constant.common.TimeConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Boundary converter between the business {@link PointValueBO} (wall-clock
 * {@code LocalDateTime}) and the port's {@link PointValueSample} (absolute
 * {@code Instant}). Both directions are pinned to {@link TimeConstant#DEFAULT_ZONEID}
 * — the same zone the relational latest-projection type handler writes with —
 * so a sample stored through the port and its projection written through the
 * mapper hold the same instant. Never let the JVM default zone leak in here.
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Component
public class PointValueSampleConverter {

    /** BO wall-clock → absolute instant, pinned to the platform zone. */
    public Instant toInstant(LocalDateTime wallClock) {
        return Objects.isNull(wallClock) ? null : wallClock.atZone(TimeConstant.DEFAULT_ZONEID).toInstant();
    }

    /** Absolute instant → BO wall-clock, pinned to the platform zone. */
    public LocalDateTime toWallClock(Instant instant) {
        return Objects.isNull(instant) ? null : LocalDateTime.ofInstant(instant, TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Business BO → port sample. Quality defaults to 0 (GOOD) — the business
     * layer does not model quality yet; when it does, this is the injection point.
     */
    public PointValueSample toSample(PointValueBO valueBO) {
        return new PointValueSample(
                new SeriesKey(valueBO.getTenantId(), valueBO.getDeviceId(), valueBO.getPointId()),
                toInstant(valueBO.getCreateTime()),
                toInstant(valueBO.getOperateTime()),
                valueBO.getRawValue(), valueBO.getCalValue(), valueBO.getNumValue(),
                0,
                valueBO.getMessageId(), valueBO.getSchemaVersion(),
                valueBO.getDriverNode(), valueBO.getSequence(),
                valueBO.getFencingToken(), valueBO.getDriverId());
    }

    /** Port sample → business BO; timestamps return to wall-clock in the platform zone. */
    public PointValueBO toBO(PointValueSample sample) {
        return PointValueBO.builder()
                .tenantId(sample.series().tenantId())
                .deviceId(sample.series().deviceId())
                .pointId(sample.series().pointId())
                .messageId(sample.messageId())
                .schemaVersion(sample.schemaVersion())
                .driverNode(sample.driverNode())
                .sequence(sample.sequence())
                .fencingToken(sample.fencingToken())
                .rawValue(sample.rawValue())
                .calValue(sample.calValue())
                .numValue(sample.numericValue())
                .driverId(sample.driverId())
                .createTime(toWallClock(sample.deviceTime()))
                .operateTime(toWallClock(sample.receiveTime()))
                .build();
    }

    /** Batch form of {@link #toSample(PointValueBO)}, order-preserving. */
    public List<PointValueSample> toSamples(List<PointValueBO> values) {
        return values.stream().map(this::toSample).toList();
    }

    /** Batch form of {@link #toBO(PointValueSample)}, order-preserving. */
    public List<PointValueBO> toBOs(List<PointValueSample> samples) {
        return samples.stream().map(this::toBO).toList();
    }
}
