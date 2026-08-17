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
import io.github.pnoker.common.driver.entity.dto.CommandAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory holder for driver registration state and shared metadata used across the
 * driver runtime.
 *
 * <p>The leased device set and the four attribute maps are read from multiple threads
 * at the same time while Quartz worker threads iterate the current ownership snapshot.
 * Attribute maps are also mutated during driver metadata refresh.
 * The fields therefore use thread-safe implementations and the setters copy contents
 * into the existing collection instead of swapping the reference, so callers that
 * already hold a reference (e.g. via
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

    /** Fencing tokens for devices currently owned by this runtime node. */
    private final Map<Long, Long> deviceFencingTokens = new ConcurrentHashMap<>();

    /** Manager-issued instance lease deadline. */
    private volatile long leaseUntilEpochMillis;

    /** Manager assignment generation currently installed in this runtime. */
    private volatile long assignmentVersion;
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
     * Command attributes keyed by attribute identifier.
     */
    private final Map<Long, CommandAttributeDTO> commandAttributeIdMap = new ConcurrentHashMap<>();
    /**
     * Command attributes keyed by attribute code.
     */
    private final Map<String, CommandAttributeDTO> commandAttributeNameMap = new ConcurrentHashMap<>();
    /**
     * Event attributes keyed by attribute identifier.
     */
    private final Map<Long, EventAttributeDTO> eventAttributeIdMap = new ConcurrentHashMap<>();
    /**
     * Event attributes keyed by attribute code.
     */
    private final Map<String, EventAttributeDTO> eventAttributeNameMap = new ConcurrentHashMap<>();
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

    /**
     * Unmodifiable view of the device ids so callers cannot mutate the internal set
     * through the getter. The underlying set is still live — reads observe the most
     * recent state. Ownership can only be replaced with a Manager-issued lease snapshot.
     *
     * @return unmodifiable live view of the device ids
     */
    public Set<Long> getDeviceIds() {
        return leaseValid() ? Collections.unmodifiableSet(deviceIds) : Collections.emptySet();
    }

    /** Atomically replace owned devices and publish the new lease deadline. */
    public synchronized void setDeviceLeases(Map<Long, Long> leases, long leaseUntilEpochMillis,
                                             long assignmentVersion) {
        deviceFencingTokens.clear();
        deviceIds.clear();
        if (Objects.nonNull(leases)) {
            deviceFencingTokens.putAll(leases);
            deviceIds.addAll(leases.keySet());
        }
        this.leaseUntilEpochMillis = leaseUntilEpochMillis;
        this.assignmentVersion = assignmentVersion;
    }

    /** Extend the instance deadline without retransmitting an unchanged assignment. */
    public void renewLeaseDeadline(long leaseUntilEpochMillis) {
        this.leaseUntilEpochMillis = leaseUntilEpochMillis;
    }

    public boolean ownsDevice(Long deviceId) {
        return leaseValid() && deviceFencingTokens.containsKey(deviceId);
    }

    public Long getFencingToken(Long deviceId) {
        return ownsDevice(deviceId) ? deviceFencingTokens.get(deviceId) : null;
    }

    public boolean leaseValid() {
        return System.currentTimeMillis() < leaseUntilEpochMillis;
    }

    /**
     * Remove a device id from the live set.
     *
     * @param id device id to remove
     * @return {@code true} if the set contained the id
     */
    public boolean removeDeviceId(Long id) {
        deviceFencingTokens.remove(id);
        return deviceIds.remove(id);
    }

    /**
     * Replaces the contents of the driver attribute map keyed by id in place so existing references stay valid.
     *
     * @param driverAttributeIdMap driver attributes to publish; {@code null} clears the map
     */
    public void setDriverAttributeIdMap(Map<Long, DriverAttributeDTO> driverAttributeIdMap) {
        replaceContents(this.driverAttributeIdMap, driverAttributeIdMap);
    }

    /**
     * Replaces the contents of the driver attribute map keyed by code in place so existing references stay valid.
     *
     * @param driverAttributeNameMap driver attributes to publish; {@code null} clears the map
     */
    public void setDriverAttributeNameMap(Map<String, DriverAttributeDTO> driverAttributeNameMap) {
        replaceContents(this.driverAttributeNameMap, driverAttributeNameMap);
    }

    /**
     * Replaces the contents of the point attribute map keyed by id in place so existing references stay valid.
     *
     * @param pointAttributeIdMap point attributes to publish; {@code null} clears the map
     */
    public void setPointAttributeIdMap(Map<Long, PointAttributeDTO> pointAttributeIdMap) {
        replaceContents(this.pointAttributeIdMap, pointAttributeIdMap);
    }

    /**
     * Replaces the contents of the point attribute map keyed by code in place so existing references stay valid.
     *
     * @param pointAttributeNameMap point attributes to publish; {@code null} clears the map
     */
    public void setPointAttributeNameMap(Map<String, PointAttributeDTO> pointAttributeNameMap) {
        replaceContents(this.pointAttributeNameMap, pointAttributeNameMap);
    }

    /**
     * Replaces the contents of the command attribute map keyed by id in place so existing references stay valid.
     *
     * @param commandAttributeIdMap command attributes to publish; {@code null} clears the map
     */
    public void setCommandAttributeIdMap(Map<Long, CommandAttributeDTO> commandAttributeIdMap) {
        replaceContents(this.commandAttributeIdMap, commandAttributeIdMap);
    }

    /**
     * Replaces the contents of the command attribute map keyed by code in place so existing references stay valid.
     *
     * @param commandAttributeNameMap command attributes to publish; {@code null} clears the map
     */
    public void setCommandAttributeNameMap(Map<String, CommandAttributeDTO> commandAttributeNameMap) {
        replaceContents(this.commandAttributeNameMap, commandAttributeNameMap);
    }

    /**
     * Replaces the contents of the event attribute map keyed by id in place so existing references stay valid.
     *
     * @param eventAttributeIdMap event attributes to publish; {@code null} clears the map
     */
    public void setEventAttributeIdMap(Map<Long, EventAttributeDTO> eventAttributeIdMap) {
        replaceContents(this.eventAttributeIdMap, eventAttributeIdMap);
    }

    /**
     * Replaces the contents of the event attribute map keyed by code in place so existing references stay valid.
     *
     * @param eventAttributeNameMap event attributes to publish; {@code null} clears the map
     */
    public void setEventAttributeNameMap(Map<String, EventAttributeDTO> eventAttributeNameMap) {
        replaceContents(this.eventAttributeNameMap, eventAttributeNameMap);
    }

    /**
     * Clears every metadata collection, sets the registered driver to {@code null}, and resets
     * the driver status to {@link EntityStatusEnum#OFFLINE}.
     */
    public void clear() {
        deviceIds.clear();
        deviceFencingTokens.clear();
        leaseUntilEpochMillis = 0;
        assignmentVersion = 0;
        driverAttributeIdMap.clear();
        driverAttributeNameMap.clear();
        pointAttributeIdMap.clear();
        pointAttributeNameMap.clear();
        commandAttributeIdMap.clear();
        commandAttributeNameMap.clear();
        eventAttributeIdMap.clear();
        eventAttributeNameMap.clear();
        driver = null;
        driverStatus = EntityStatusEnum.OFFLINE;
    }

}
