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
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.dto.EventReportDTO;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    static Map<String, Object> deviceAlarm(DeviceAlarmDTO alarm) {
        return RuleValueMap.from(new DeviceAlarmSnapshot(
                alarm.getDeviceId(),
                alarm.getDriverId(),
                alarm.getStatus(),
                alarm.getStatusName(),
                alarm.getMessage(),
                alarm.getAlarmId(),
                alarm.getCreateTime()));
    }

    static Map<String, Object> driverAlarm(DriverAlarmDTO alarm) {
        return RuleValueMap.from(new DriverAlarmSnapshot(
                alarm.getDriverId(),
                alarm.getStatus(),
                alarm.getStatusName(),
                alarm.getMessage(),
                alarm.getAlarmId(),
                alarm.getCreateTime()));
    }

    static Map<String, Object> eventReport(EventReportDTO dto) {
        LocalDateTime ts = Objects.nonNull(dto.occurTime())
                ? LocalDateTime.ofInstant(dto.occurTime(), ZoneId.systemDefault())
                : LocalDateTime.now();
        return RuleValueMap.from(new EventReportSnapshot(
                dto.deviceId(),
                dto.eventId(),
                dto.eventCode(),
                dto.eventTypeFlag(),
                dto.eventLevelFlag(),
                dto.paramValues(),
                dto.message(),
                ts));
    }

    private record EventReportSnapshot(
            Long deviceId,
            Long eventId,
            String eventCode,
            Byte eventTypeFlag,
            Byte eventLevelFlag,
            Map<String, String> paramValues,
            String message,
            LocalDateTime occurTime) {
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

    private record DeviceAlarmSnapshot(
            Long deviceId,
            Long driverId,
            String status,
            String statusName,
            String message,
            Long alarmId,
            LocalDateTime createTime) {
    }

    private record DriverAlarmSnapshot(
            Long driverId,
            String status,
            String statusName,
            String message,
            Long alarmId,
            LocalDateTime createTime) {
    }

}
