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
package io.github.pnoker.common.agentic.tool;

import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * Delegates to the facade layer ({@link PointValueFacade} and
 * {@link PointValueCommandFacade}) so that calls follow the project's local/gRPC
 * dual-mode convention.
 */
@Slf4j
@Component
public class DataToolSet {

    private final PointValueFacade pointValueFacade;

    private final PointValueCommandFacade pointValueCommandFacade;

    public DataToolSet(PointValueFacade pointValueFacade, PointValueCommandFacade pointValueCommandFacade) {
        this.pointValueFacade = pointValueFacade;
        this.pointValueCommandFacade = pointValueCommandFacade;
    }

    @Tool(description = "Get the latest point value for a specific device and point. Returns the current value.")
    public String getLatestPointValue(@ToolParam(description = "The tenant ID") Long tenantId,
                                      @ToolParam(description = "The device ID") Long deviceId,
                                      @ToolParam(description = "The point (metric) ID") Long pointId) {
        log.debug("Tool: getLatestPointValue(tenantId={}, deviceId={}, pointId={})", tenantId, deviceId, pointId);
        try {
            FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, pointId);
            if (value == null) {
                return "No latest value found for device " + deviceId + " point " + pointId;
            }
            return String.format("Device %d / Point %d: value=%s, rawValue=%s, time=%d", value.getDeviceId(),
                    value.getPointId(), value.getValue(), value.getRawValue(), value.getCreateTime());
        } catch (Exception e) {
            log.error("Failed to get latest point value: {}", e.getMessage());
            return "Error retrieving latest value: " + e.getMessage();
        }
    }

    @Tool(description = "Get historical point values for a specific device and point. Returns a list of value strings.")
    public String getPointValueHistory(@ToolParam(description = "The tenant ID") Long tenantId,
                                       @ToolParam(description = "The device ID") Long deviceId,
                                       @ToolParam(description = "The point (metric) ID") Long pointId,
                                       @ToolParam(description = "Number of historical records to retrieve") int count) {
        log.debug("Tool: getPointValueHistory(tenantId={}, deviceId={}, pointId={}, count={})", tenantId, deviceId,
                pointId, count);
        try {
            List<String> history = pointValueFacade.history(tenantId, deviceId, pointId, count);
            if (history == null || history.isEmpty()) {
                return "No history data found for device " + deviceId + " point " + pointId;
            }
            return "History values (" + history.size() + " records): " + String.join(", ", history);
        } catch (Exception e) {
            log.error("Failed to get point value history: {}", e.getMessage());
            return "Error retrieving history: " + e.getMessage();
        }
    }

    @Tool(description = "Send a read command to a device for a specific point. The driver will read the current value from the physical device.")
    public String readPointValue(@ToolParam(description = "The device ID") Long deviceId,
                                 @ToolParam(description = "The point (metric) ID to read") Long pointId) {
        log.debug("Tool: readPointValue(deviceId={}, pointId={})", deviceId, pointId);
        try {
            boolean success = pointValueCommandFacade.read(deviceId, pointId);
            return success ? "Read command sent successfully for device " + deviceId + " point " + pointId
                    : "Read command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.error("Failed to send read command: {}", e.getMessage());
            return "Error sending read command: " + e.getMessage();
        }
    }

    @Tool(description = "Send a write command to a device for a specific point. Sets the point to the specified value on the physical device.")
    public String writePointValue(@ToolParam(description = "The device ID") Long deviceId,
                                  @ToolParam(description = "The point (metric) ID to write") Long pointId,
                                  @ToolParam(description = "The value to write (as a string)") String value) {
        log.debug("Tool: writePointValue(deviceId={}, pointId={}, value={})", deviceId, pointId, value);
        try {
            boolean success = pointValueCommandFacade.write(deviceId, pointId, value);
            return success
                    ? "Write command sent successfully for device " + deviceId + " point " + pointId + " value=" + value
                    : "Write command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.error("Failed to send write command: {}", e.getMessage());
            return "Error sending write command: " + e.getMessage();
        }
    }

}
