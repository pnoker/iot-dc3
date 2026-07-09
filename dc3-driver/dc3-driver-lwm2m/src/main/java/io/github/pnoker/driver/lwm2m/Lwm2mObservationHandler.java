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

package io.github.pnoker.driver.lwm2m;

import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * LwM2M Observation Handler.
 * <p>
 * Handles LwM2M Observe notifications by looking up the corresponding device and point
 * from the driver metadata, then forwarding the value via the driver sender service.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Lwm2mObservationHandler {

    private final DriverMetadata driverMetadata;
    private final DeviceMetadata deviceMetadata;
    private final DriverSenderService driverSenderService;

    /**
     * Handle an observed value change from a LwM2M device resource.
     *
     * @param endpoint         device endpoint name
     * @param objectId         LwM2M Object ID
     * @param objectInstanceId LwM2M Object Instance ID
     * @param resourceId       LwM2M Resource ID
     * @param value            observed value as string
     */
    public void onObservation(String endpoint, int objectId, int objectInstanceId, int resourceId, String value) {
        if (Objects.isNull(value)) {
            return;
        }

        log.debug("LwM2M observation received: endpoint={}, path=/{}/{}/{}, value={}",
                endpoint, objectId, objectInstanceId, resourceId, value);

        // TODO: Implement device/point lookup from driver metadata using endpoint and resource path.
        //  The current driver metadata model uses deviceId-based lookups, not endpoint-based.
        //  A future enhancement should maintain an endpoint -> deviceId mapping and
        //  objectId/objectInstanceId/resourceId -> pointId mapping to enable automatic
        //  point value forwarding from observed resources.
        log.debug("LwM2M observation value received but auto-forwarding not yet implemented: endpoint={}, path=/{}/{}/{}",
                endpoint, objectId, objectInstanceId, resourceId);
    }
}
