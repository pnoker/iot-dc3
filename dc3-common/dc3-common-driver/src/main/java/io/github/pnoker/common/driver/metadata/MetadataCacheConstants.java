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

package io.github.pnoker.common.driver.metadata;

import io.github.pnoker.common.constant.common.BaseConstant;

/**
 * Shared cache parameters for the driver-side metadata caches
 * ({@link DeviceMetadata}, {@link PointMetadata}). Kept here so a tuning change
 * in one cache cannot drift away from the other.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public final class MetadataCacheConstants {

    /**
     * Upper bound on a single cache lookup so a stuck manager center cannot pin
     * driver worker threads forever; expired waits return {@code null} and let
     * callers move on instead of holding the Quartz / RabbitMQ thread hostage.
     */
    public static final long CACHE_LOAD_TIMEOUT_SECONDS = 5L;

    /**
     * Soft cap on cached entries. Eviction is purely size-based — freshness is
     * driven by RabbitMQ metadata events, not by time-based expiration.
     */
    public static final long MAX_CACHE_SIZE = 5000L;

    private MetadataCacheConstants() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
