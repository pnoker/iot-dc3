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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.entity.dto.MetadataEventDTO;

/**
 * Driver-side reaction to driver, device, and point metadata events forwarded
 * from the manager center. Implement this when the driver needs to refresh
 * caches, recompute derived state, or open/close protocol connections in
 * response to an add / update / delete on the metadata stream — the
 * specific subject is carried in {@link MetadataEventDTO}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2025.9.0
 */
public interface DriverMetadataListener {

    /**
     * Receive a metadata change event for driver, device, or point.
     *
     * @param metadataEvent metadata event payload describing the affected
     *                      entity, change type, and identifiers
     */
    void event(MetadataEventDTO metadataEvent);

}
