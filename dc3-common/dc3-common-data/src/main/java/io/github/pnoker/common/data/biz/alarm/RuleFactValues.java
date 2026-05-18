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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime fact value snapshots used by the rule engine.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
final class RuleFactValues {

    private RuleFactValues() {
    }

    static Map<String, Object> point(PointValueBO pointValue) {
        return RuleValueMap.from(new PointSnapshot(
                pointValue.getDeviceId(),
                pointValue.getPointId(),
                pointValue.getDriverId(),
                pointValue.getRawValue(),
                pointValue.getCalValue(),
                pointValue.getCalValue(),
                pointValue.getNumValue(),
                pointValue.getCreateTime(),
                pointValue.getOperateTime()));
    }

    static Map<String, Object> device(DeviceEventDTO.DeviceStatus payload, DeviceEventTypeEnum eventType, String source,
                                      Long eventId) {
        return RuleValueMap.from(new DeviceEventSnapshot(
                payload.getDeviceId(),
                payload.getDriverId(),
                Objects.nonNull(payload.getStatus()) ? payload.getStatus().getCode() : null,
                Objects.nonNull(payload.getStatus()) ? payload.getStatus().name() : null,
                payload.getMessage(),
                Objects.nonNull(eventType) ? eventType.getCode() : null,
                source,
                eventId,
                payload.getCreateTime()));
    }

    static Map<String, Object> driver(DriverEventDTO.DriverStatus payload, DriverEventTypeEnum eventType, String source,
                                      Long eventId) {
        return RuleValueMap.from(new DriverEventSnapshot(
                payload.getDriverId(),
                Objects.nonNull(payload.getStatus()) ? payload.getStatus().getCode() : null,
                Objects.nonNull(payload.getStatus()) ? payload.getStatus().name() : null,
                payload.getMessage(),
                Objects.nonNull(eventType) ? eventType.getCode() : null,
                source,
                eventId,
                payload.getCreateTime()));
    }

    private record PointSnapshot(
            Long deviceId,
            Long pointId,
            Long driverId,
            String rawValue,
            String calValue,
            String value,
            Double numValue,
            LocalDateTime createTime,
            LocalDateTime operateTime) {
    }

    private record DeviceEventSnapshot(
            Long deviceId,
            Long driverId,
            String status,
            String statusName,
            String message,
            String eventType,
            String source,
            Long eventId,
            LocalDateTime createTime) {
    }

    private record DriverEventSnapshot(
            Long driverId,
            String status,
            String statusName,
            String message,
            String eventType,
            String source,
            Long eventId,
            LocalDateTime createTime) {
    }

}
