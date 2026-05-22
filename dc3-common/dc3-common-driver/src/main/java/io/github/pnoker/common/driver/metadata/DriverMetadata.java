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

import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory holder for driver registration state and shared metadata used across the
 * driver runtime.
 *
 * <p>The {@code deviceIds} set and the four attribute maps are mutated from multiple
 * threads at the same time — RabbitMQ consumer threads add/remove entries as
 * metadata events arrive while Quartz worker threads iterate the same collections
 * during read scans. The fields therefore use thread-safe implementations and the
 * setters copy contents into the existing collection instead of swapping the
 * reference, so callers that already hold a reference (e.g. via
 * {@code getDeviceIds()}) continue to see the live state.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@ToString
@Component
public final class DriverMetadata {

    /**
     * Identifiers of devices owned by the driver.
     */
    private final Set<Long> deviceIds = ConcurrentHashMap.newKeySet();
    /**
     * Driver attributes keyed by attribute identifier.
     */
    private final Map<Long, DriverAttributeDTO> driverAttributeIdMap = new ConcurrentHashMap<>();
    /**
     * Driver attributes keyed by attribute code.
     */
    private final Map<String, DriverAttributeDTO> driverAttributeNameMap = new ConcurrentHashMap<>();
    /**
     * Point attributes keyed by attribute identifier.
     */
    private final Map<Long, PointAttributeDTO> pointAttributeIdMap = new ConcurrentHashMap<>();
    /**
     * Point attributes keyed by attribute code.
     */
    private final Map<String, PointAttributeDTO> pointAttributeNameMap = new ConcurrentHashMap<>();
    /**
     * Current driver status.
     */
    @Setter
    private volatile EntityStatusEnum driverStatus = EntityStatusEnum.OFFLINE;
    /**
     * Registered driver definition.
     */
    @Setter
    private volatile DriverBO driver;

    private static <E> void replaceContents(Set<E> target, Set<E> source) {
        target.clear();
        if (Objects.nonNull(source)) {
            target.addAll(source);
        }
    }

    private static <K, V> void replaceContents(Map<K, V> target, Map<K, V> source) {
        target.clear();
        if (Objects.nonNull(source)) {
            target.putAll(source);
        }
    }

    public void setDeviceIds(Set<Long> deviceIds) {
        replaceContents(this.deviceIds, deviceIds);
    }

    public void setDriverAttributeIdMap(Map<Long, DriverAttributeDTO> driverAttributeIdMap) {
        replaceContents(this.driverAttributeIdMap, driverAttributeIdMap);
    }

    public void setDriverAttributeNameMap(Map<String, DriverAttributeDTO> driverAttributeNameMap) {
        replaceContents(this.driverAttributeNameMap, driverAttributeNameMap);
    }

    public void setPointAttributeIdMap(Map<Long, PointAttributeDTO> pointAttributeIdMap) {
        replaceContents(this.pointAttributeIdMap, pointAttributeIdMap);
    }

    public void setPointAttributeNameMap(Map<String, PointAttributeDTO> pointAttributeNameMap) {
        replaceContents(this.pointAttributeNameMap, pointAttributeNameMap);
    }

    public void clear() {
        deviceIds.clear();
        driverAttributeIdMap.clear();
        driverAttributeNameMap.clear();
        pointAttributeIdMap.clear();
        pointAttributeNameMap.clear();
        driver = null;
        driverStatus = EntityStatusEnum.OFFLINE;
    }

}
