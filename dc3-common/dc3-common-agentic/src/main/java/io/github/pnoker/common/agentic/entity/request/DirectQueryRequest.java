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
package io.github.pnoker.common.agentic.entity.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Explicit deterministic query payload for server-side agentic lookups.
 * <p>
 * This object is intentionally separate from the free-form chat message. Direct backend
 * lookups only run when clients provide structured selectors here; natural language is
 * handled by the normal model/tool path.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectQueryRequest {

    public static final String TYPE_POINT_VALUE = "point-value";

    /**
     * Query type. Currently supports {@code point-value}.
     */
    private String type;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private Long pointId;

    private String pointName;

    private String pointCode;

    /**
     * Number of latest values to return. Clamped to 1..50 by the backend.
     */
    private Integer limit;

    public boolean isPointValueQuery() {
        return TYPE_POINT_VALUE.equalsIgnoreCase(StringUtils.trimToEmpty(type));
    }

    public boolean hasDeviceSelector() {
        return Objects.nonNull(deviceId) || StringUtils.isNotBlank(deviceName) || StringUtils.isNotBlank(deviceCode);
    }

    public boolean hasPointSelector() {
        return Objects.nonNull(pointId) || StringUtils.isNotBlank(pointName) || StringUtils.isNotBlank(pointCode);
    }

    public int normalizedLimit() {
        if (Objects.isNull(limit)) {
            return 1;
        }
        return Math.max(1, Math.min(limit, 50));
    }

}
