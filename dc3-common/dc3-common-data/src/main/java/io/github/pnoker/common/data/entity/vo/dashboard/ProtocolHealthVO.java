/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.entity.vo.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Rollup for one driver service_name — how many driver rows are enabled,
 * how many devices they serve, how much sample volume they carried in
 * the window. Phase-1 health signal is just enable% + device count +
 * sample volume; true heartbeat/latency signals get layered in later.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProtocolHealthVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * e.g. {@code dc3-driver-modbus-tcp}; frontend strips the prefix.
     */
    private String serviceName;
    private long driverCount;
    private long enabledCount;
    private long deviceCount;
    /**
     * Sum of pv samples over the default window (24h). 0 if none in window.
     */
    private long sampleVolume;
}
