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

/**
 * Converts runtime data into rule facts and feeds the alarm rule pipeline.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface AlarmRuleTriggerService {

    /**
     * Process a point value sample.
     *
     * @param pointValue point value
     */
    void processPointValue(PointValueBO pointValue);

    /**
     * Process a device runtime event.
     *
     * @param payload   device status payload
     * @param eventType source event type
     * @param source    source label stored in fact values
     * @param eventId   persisted event id when one already exists
     */
    void processDeviceEvent(DeviceEventDTO.DeviceStatus payload, DeviceEventTypeEnum eventType, String source,
                            Long eventId);

    /**
     * Process a driver runtime event.
     *
     * @param payload   driver status payload
     * @param eventType source event type
     * @param source    source label stored in fact values
     * @param eventId   persisted event id when one already exists
     */
    void processDriverEvent(DriverEventDTO.DriverStatus payload, DriverEventTypeEnum eventType, String source,
                            Long eventId);

}
