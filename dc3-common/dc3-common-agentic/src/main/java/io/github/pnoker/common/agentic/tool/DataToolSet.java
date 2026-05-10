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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Data-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * Delegates to the facade layer ({@link PointValueFacade} and
 * {@link PointValueCommandFacade}) so that calls follow the project's local/gRPC
 * dual-mode convention.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DataToolSet {

    private final PointValueFacade pointValueFacade;

    private final PointValueCommandFacade pointValueCommandFacade;

    private final ActionService actionService;

    public DataToolSet(PointValueFacade pointValueFacade, PointValueCommandFacade pointValueCommandFacade,
                       ActionService actionService) {
        this.pointValueFacade = pointValueFacade;
        this.pointValueCommandFacade = pointValueCommandFacade;
        this.actionService = actionService;
    }

    @Tool(description = "Get the latest point value for a specific device and point. Returns the current value.")
    public String getLatestPointValue(@ToolParam(description = "The device ID") Long deviceId,
                                      @ToolParam(description = "The point (metric) ID") Long pointId,
                                      ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                tenantId, deviceId, pointId);
        recordTool(toolContext, "getLatestPointValue", "Get latest point value");
        try {
            FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, pointId);
            if (Objects.isNull(value)) {
                return "No latest value found for device " + deviceId + " point " + pointId;
            }
            return String.format("Device %d / Point %d: value=%s, rawValue=%s, time=%d", value.getDeviceId(),
                    value.getPointId(), value.getValue(), value.getRawValue(), value.getCreateTime());
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                    tenantId, deviceId, pointId, e);
            return "Error retrieving latest value: " + e.getMessage();
        }
    }

    @Tool(description = "Get historical point values for a specific device and point. Returns a list of value strings.")
    public String getPointValueHistory(@ToolParam(description = "The device ID") Long deviceId,
                                       @ToolParam(description = "The point (metric) ID") Long pointId,
                                       @ToolParam(description = "Number of historical records to retrieve") int count,
                                       ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                "getPointValueHistory", tenantId, deviceId, pointId, count);
        recordTool(toolContext, "getPointValueHistory", "Get point value history");
        try {
            List<String> history = pointValueFacade.history(tenantId, deviceId, pointId, count);
            if (Objects.isNull(history) || history.isEmpty()) {
                return "No history data found for device " + deviceId + " point " + pointId;
            }
            return "History values (" + history.size() + " records): " + String.join(", ", history);
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                    "getPointValueHistory", tenantId, deviceId, pointId, count, e);
            return "Error retrieving history: " + e.getMessage();
        }
    }

    @Tool(description = "Send a read command to a device for a specific point. The driver will read the current value from the physical device.")
    public String readPointValue(@ToolParam(description = "The device ID") Long deviceId,
                                 @ToolParam(description = "The point (metric) ID to read") Long pointId,
                                 ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                deviceId, pointId);
        recordTool(toolContext, "readPointValue", "Send point read command");
        try {
            boolean success = pointValueCommandFacade.read(tenantId, deviceId, pointId);
            return success ? "Read command sent successfully for device " + deviceId + " point " + pointId
                    : "Read command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                    deviceId, pointId, e);
            return "Error sending read command: " + e.getMessage();
        }
    }

    @Tool(description = "Send a write command to a device for a specific point. Sets the point to the specified value on the physical device.")
    public String writePointValue(@ToolParam(description = "The device ID") Long deviceId,
                                  @ToolParam(description = "The point (metric) ID to write") Long pointId,
                                  @ToolParam(description = "The value to write (as a string)") String value,
                                  ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, valueLength={}",
                "writePointValue", tenantId, deviceId, pointId, Objects.isNull(value) ? 0 : value.length());
        recordTool(toolContext, "writePointValue", "Prepare point write command");
        try {
            if (AgenticRequestContext.confirmActions(toolContext)) {
                RequestHeader.UserHeader header = AgenticRequestContext.requireUserHeader();
                String conversationId = AgenticRequestContext.requireConversationId(toolContext);
                String actionId = actionService.createWritePointValueAction(conversationId, deviceId, pointId, value,
                        header);
                return "Write command is pending user confirmation. actionId=" + actionId
                        + ". Ask the user to confirm before executing it.";
            }
            boolean success = pointValueCommandFacade.write(tenantId, deviceId, pointId, value);
            return success
                    ? "Write command sent successfully for device " + deviceId + " point " + pointId + " value=" + value
                    : "Write command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "writePointValue", tenantId,
                    deviceId, pointId, e);
            return "Error sending write command: " + e.getMessage();
        }
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "data", description);
    }

}
