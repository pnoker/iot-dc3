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
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Converts runtime data into rule facts and feeds the alarm rule pipeline.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface AlarmRuleTriggerService {

    /**
     * Process a point value sample.
     *
     * @param pointValue point value
     */
    Mono<Void> processPointValue(PointValueBO pointValue);

    /**
     * Process a batch of point value samples. Implementations should group
     * samples by {@code (tenantId, pointId)} so the candidate-rule lookup
     * runs once per group rather than per sample.
     *
     * @param pointValues point values
     */
    Mono<Void> processPointValues(List<PointValueBO> pointValues);

    /**
     * Process a device alarm event.
     *
     * @param alarm device alarm payload
     */
    Mono<Void> processDeviceAlarm(DeviceAlarmDTO alarm);

    /**
     * Process a driver alarm event.
     *
     * @param alarm driver alarm payload
     */
    Mono<Void> processDriverAlarm(DriverAlarmDTO alarm);

    /**
     * Process an event report.
     *
     * @param entityDTO event report payload
     */
    Mono<Void> processEventReport(EventReportDTO entityDTO);
}
