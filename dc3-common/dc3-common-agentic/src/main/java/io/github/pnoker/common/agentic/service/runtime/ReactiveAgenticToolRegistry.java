/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.runtime;

import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import io.github.pnoker.common.agentic.tools.CommandTool;
import io.github.pnoker.common.agentic.tools.EventTool;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.LinkedHashMap;

/** Registry for tools that must remain non-blocking end to end. */
@Component
public class ReactiveAgenticToolRegistry {

    private static final ToolDefinition WRITE_POINT_VALUE = DefaultToolDefinition.builder()
            .name("writePointValue")
            .description("Prepare a point write command for explicit user confirmation.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"},"value":{"type":"string"}},"required":["deviceId","pointId","value"]}
                    """)
            .build();
    private static final ToolDefinition READ_POINT_VALUE = DefaultToolDefinition.builder()
            .name("readPointValue")
            .description("Send a point read command and return its command identifier.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"}},"required":["deviceId","pointId"]}
                    """)
            .build();
    private static final ToolDefinition GET_LATEST_POINT_VALUE = DefaultToolDefinition.builder()
            .name("getLatestPointValue")
            .description("Get the latest value for a device point without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"}},"required":["deviceId","pointId"]}
                    """)
            .build();
    private static final ToolDefinition GET_POINT_VALUE_HISTORY = DefaultToolDefinition.builder()
            .name("getPointValueHistory")
            .description("Get recent point values and chart-ready numeric summaries without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"},"count":{"type":"integer","minimum":1}},"required":["deviceId","pointId","count"]}
                    """)
            .build();
    private static final ToolDefinition SEARCH_POINTS = DefaultToolDefinition.builder()
            .name("searchPoints")
            .description("Search tenant-scoped points with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"pointName":{"type":"string"},"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_POINTS_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listPointsByDevice")
            .description("List points bound to a device with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_POINTS_BY_PROFILE = DefaultToolDefinition.builder()
            .name("listPointsByProfile")
            .description("List points under a profile with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["profileId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_POINT = DefaultToolDefinition.builder()
            .name("lookupPointById").description("Look up one tenant-scoped point without blocking the agent runtime.")
            .inputSchema("{\"type\":\"object\",\"properties\":{\"pointId\":{\"type\":\"integer\"}},\"required\":[\"pointId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_POINTS = DefaultToolDefinition.builder()
            .name("lookupPointsByIds").description("Look up tenant-scoped points by IDs without blocking the agent runtime.")
            .inputSchema("{\"type\":\"object\",\"properties\":{\"pointIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"pointIds\"]}")
            .build();
    private static final ToolDefinition SEARCH_PROFILES = DefaultToolDefinition.builder()
            .name("searchProfiles")
            .description("Search tenant-scoped profiles with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileName":{"type":"string"},"profileCode":{"type":"string"},"profileType":{"type":"string"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_PROFILE = DefaultToolDefinition.builder()
            .name("lookupProfileById")
            .description("Look up one tenant-scoped profile without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"}},"required":["profileId"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_PROFILES = DefaultToolDefinition.builder()
            .name("lookupProfilesByIds")
            .description("Look up tenant-scoped profiles by IDs without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileIds":{"type":"array","items":{"type":"integer"}}},"required":["profileIds"]}
                    """)
            .build();
    private static final ToolDefinition LIST_PROFILES_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listProfilesByDeviceId")
            .description("List profiles bound to a device with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition SEARCH_COMMANDS = DefaultToolDefinition.builder().name("searchCommands").description("Search tenant-scoped commands with canonical offset pagination.").inputSchema("{\"type\":\"object\",\"properties\":{\"commandName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}").build();
    private static final ToolDefinition LOOKUP_COMMAND = DefaultToolDefinition.builder().name("lookupCommandById").description("Look up one tenant-scoped command without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"commandId\":{\"type\":\"integer\"}},\"required\":[\"commandId\"]}").build();
    private static final ToolDefinition SEARCH_EVENTS = DefaultToolDefinition.builder().name("searchEvents").description("Search tenant-scoped events with canonical offset pagination.").inputSchema("{\"type\":\"object\",\"properties\":{\"eventName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}").build();
    private static final ToolDefinition LOOKUP_EVENT = DefaultToolDefinition.builder().name("lookupEventById").description("Look up one tenant-scoped event without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"eventId\":{\"type\":\"integer\"}},\"required\":[\"eventId\"]}").build();
    private static final ToolDefinition DEVICE_LATEST_VALUES = DefaultToolDefinition.builder().name("getDeviceLatestPointValues").description("Load a device latest-point snapshot without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}},\"required\":[\"deviceId\",\"limit\"]}").build();
    private static final ToolDefinition LOOKUP_DEVICE = DefaultToolDefinition.builder().name("lookupDeviceById").description("Look up one tenant-scoped device without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"}},\"required\":[\"deviceId\"]}").build();
    private static final ToolDefinition LOOKUP_DEVICES = DefaultToolDefinition.builder().name("lookupDevicesByIds").description("Look up tenant-scoped devices by IDs without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"deviceIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"deviceIds\"]}").build();
    private static final ToolDefinition SEARCH_DEVICES = DefaultToolDefinition.builder().name("searchDevices").description("Search tenant-scoped devices with canonical offset pagination.").inputSchema("{\"type\":\"object\",\"properties\":{\"deviceName\":{\"type\":\"string\"},\"deviceCode\":{\"type\":\"string\"},\"driverId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}").build();
    private static final ToolDefinition LIST_DEVICES_BY_DRIVER = DefaultToolDefinition.builder().name("listDevicesByDriverId").description("List devices for a driver with canonical offset pagination without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"driverId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"driverId\",\"offset\",\"limit\"]}").build();
    private static final ToolDefinition LIST_DEVICES_BY_PROFILE = DefaultToolDefinition.builder().name("listDevicesByProfileId").description("List devices for a profile with canonical offset pagination without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"profileId\",\"offset\",\"limit\"]}").build();
    private static final ToolDefinition LOOKUP_DRIVER = DefaultToolDefinition.builder().name("lookupDriverById").description("Look up one tenant-scoped driver without blocking.").inputSchema("{\"type\":\"object\",\"properties\":{\"driverId\":{\"type\":\"integer\"}},\"required\":[\"driverId\"]}").build();
    private static final ToolDefinition SEARCH_DRIVERS = DefaultToolDefinition.builder().name("searchDrivers").description("Search tenant-scoped drivers with canonical offset pagination.").inputSchema("{\"type\":\"object\",\"properties\":{\"driverName\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}").build();

    private final PointValueTool pointValueTool;
    private final PointTool pointTool;
    private final ProfileTool profileTool;
    private final CommandTool commandTool;
    private final EventTool eventTool;
    private final DeviceTool deviceTool;
    private final DriverTool driverTool;
    private final ObjectMapper objectMapper;

    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, ObjectMapper objectMapper) {
        this(pointValueTool, null, null, null, null, null, null, objectMapper);
    }

    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, PointTool pointTool,
                                       ObjectMapper objectMapper) {
        this(pointValueTool, pointTool, null, null, null, null, null, objectMapper);
    }

    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, PointTool pointTool,
                                       ProfileTool profileTool, ObjectMapper objectMapper) {
        this(pointValueTool, pointTool, profileTool, null, null, null, null, objectMapper);
    }

    @Autowired
    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, PointTool pointTool,
                                       ProfileTool profileTool, CommandTool commandTool,
                                       EventTool eventTool, DeviceTool deviceTool, DriverTool driverTool, ObjectMapper objectMapper) {
        this.pointValueTool = pointValueTool;
        this.pointTool = pointTool;
        this.profileTool = profileTool;
        this.commandTool = commandTool;
        this.eventTool = eventTool;
        this.deviceTool = deviceTool;
        this.driverTool = driverTool;
        this.objectMapper = objectMapper;
    }

    public Map<String, ReactiveAgenticTool> tools() {
        if (objectMapper == null) {
            return Map.of();
        }
        Map<String, ReactiveAgenticTool> tools = new LinkedHashMap<>();
        if (pointValueTool != null) {
            tools.put("writePointValue", new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return WRITE_POINT_VALUE;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                    Long deviceId = number(values.get("deviceId"));
                    Long pointId = number(values.get("pointId"));
                    String value = values.get("value") == null ? null : values.get("value").toString();
                    return pointValueTool.writePointValueReactive(deviceId, pointId, value, context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }

            private Long number(Object value) {
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return value == null ? null : Long.valueOf(value.toString());
            }
            });
            tools.put("readPointValue", new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return READ_POINT_VALUE;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                    return pointValueTool.readPointValueReactive(number(values.get("deviceId")),
                            number(values.get("pointId")), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }

            private Long number(Object value) {
                if (value instanceof Number number) return number.longValue();
                return value == null ? null : Long.valueOf(value.toString());
            }
            });
            tools.put("getLatestPointValue", new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return GET_LATEST_POINT_VALUE; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try {
                    Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                    return pointValueTool.getLatestPointValueReactive(number(values.get("deviceId")),
                            number(values.get("pointId")), context);
                } catch (RuntimeException error) { return Mono.error(error); }
            }
            private Long number(Object value) { return value instanceof Number n ? n.longValue() : value == null ? null : Long.valueOf(value.toString()); }
            });
            tools.put("getPointValueHistory", new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return GET_POINT_VALUE_HISTORY; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try {
                    Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                    Object count = values.get("count");
                    int size = count instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(count));
                    return pointValueTool.getPointValueHistoryReactive(number(values.get("deviceId")),
                            number(values.get("pointId")), size, context);
                } catch (RuntimeException error) { return Mono.error(error); }
            }
            private Long number(Object value) { return value instanceof Number n ? n.longValue() : value == null ? null : Long.valueOf(value.toString()); }
            });
        }
        if (pointTool != null) {
            tools.put("lookupPointById", pointTool(LOOKUP_POINT,
                    (values, context) -> pointTool.lookupPointByIdReactive(number(values.get("pointId")), context)));
            tools.put("lookupPointsByIds", pointTool(LOOKUP_POINTS,
                    (values, context) -> pointTool.lookupPointsByIdsReactive(numbers(values.get("pointIds")), context)));
            tools.put("searchPoints", pointTool(SEARCH_POINTS,
                    (values, context) -> pointTool.searchPointsReactive(string(values.get("pointName")),
                            number(values.get("profileId")), longValue(values.get("offset")),
                            intValue(values.get("limit")), context)));
            tools.put("listPointsByDevice", pointTool(LIST_POINTS_BY_DEVICE,
                    (values, context) -> pointTool.listPointsByDeviceIdReactive(number(values.get("deviceId")),
                            longValue(values.get("offset")), intValue(values.get("limit")), context)));
            tools.put("listPointsByProfile", pointTool(LIST_POINTS_BY_PROFILE,
                    (values, context) -> pointTool.listPointsByProfileIdReactive(number(values.get("profileId")),
                            longValue(values.get("offset")), intValue(values.get("limit")), context)));
        }
        if (profileTool != null) {
            tools.put("lookupProfileById", profileTool(LOOKUP_PROFILE,
                    (values, context) -> profileTool.lookupProfileByIdReactive(number(values.get("profileId")), context)));
            tools.put("lookupProfilesByIds", profileTool(LOOKUP_PROFILES,
                    (values, context) -> profileTool.lookupProfilesByIdsReactive(numbers(values.get("profileIds")), context)));
            tools.put("searchProfiles", profileTool(SEARCH_PROFILES,
                    (values, context) -> profileTool.searchProfilesReactive(string(values.get("profileName")),
                            string(values.get("profileCode")), string(values.get("profileType")),
                            longValue(values.get("offset")), intValue(values.get("limit")), context)));
            tools.put("listProfilesByDeviceId", profileTool(LIST_PROFILES_BY_DEVICE,
                    (values, context) -> profileTool.listProfilesByDeviceIdReactive(number(values.get("deviceId")),
                            longValue(values.get("offset")), intValue(values.get("limit")), context)));
        }
        if (commandTool != null) {
            tools.put("lookupCommandById", commandToolTool(LOOKUP_COMMAND, (values, context) -> commandTool.lookupCommandByIdReactive(number(values.get("commandId")), context)));
            tools.put("searchCommands", commandToolTool(SEARCH_COMMANDS, (values, context) -> commandTool.searchCommandsReactive(string(values.get("commandName")), number(values.get("profileId")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
        }
        if (eventTool != null) {
            tools.put("lookupEventById", eventToolTool(LOOKUP_EVENT, (values, context) -> eventTool.lookupEventByIdReactive(number(values.get("eventId")), context)));
            tools.put("searchEvents", eventToolTool(SEARCH_EVENTS, (values, context) -> eventTool.searchEventsReactive(string(values.get("eventName")), number(values.get("profileId")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
        }
        if (deviceTool != null) {
            tools.put("lookupDeviceById", deviceToolTool(LOOKUP_DEVICE, (values, context) -> deviceTool.lookupDeviceByIdReactive(number(values.get("deviceId")), context)));
            tools.put("lookupDevicesByIds", deviceToolTool(LOOKUP_DEVICES, (values, context) -> deviceTool.lookupDevicesByIdsReactive(numbers(values.get("deviceIds")), context)));
            tools.put("searchDevices", deviceToolTool(SEARCH_DEVICES, (values, context) -> deviceTool.searchDevicesReactive(string(values.get("deviceName")), string(values.get("deviceCode")), number(values.get("driverId")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
            tools.put("listDevicesByDriverId", deviceToolTool(LIST_DEVICES_BY_DRIVER, (values, context) -> deviceTool.listDevicesByDriverIdReactive(number(values.get("driverId")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
            tools.put("listDevicesByProfileId", deviceToolTool(LIST_DEVICES_BY_PROFILE, (values, context) -> deviceTool.listDevicesByProfileIdReactive(number(values.get("profileId")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
            tools.put("getDeviceLatestPointValues", deviceToolTool(DEVICE_LATEST_VALUES,
                    (values, context) -> deviceTool.getDeviceLatestPointValuesReactive(number(values.get("deviceId")), intValue(values.get("limit")), context)));
        }
        if (driverTool != null) {
            tools.put("lookupDriverById", driverToolTool(LOOKUP_DRIVER, (values, context) -> driverTool.lookupDriverByIdReactive(number(values.get("driverId")), context)));
            tools.put("searchDrivers", driverToolTool(SEARCH_DRIVERS, (values, context) -> driverTool.searchDriversReactive(string(values.get("driverName")), longValue(values.get("offset")), intValue(values.get("limit")), context)));
        }
        tools.replaceAll((name, tool) -> new ReactiveAgenticToolTracing(name, tool));
        return Map.copyOf(tools);
    }

    @FunctionalInterface
    private interface PointCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface ProfileCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface private interface CommandCall { Mono<?> invoke(Map<String, Object> values, ToolContext context); }
    @FunctionalInterface private interface EventCall { Mono<?> invoke(Map<String, Object> values, ToolContext context); }
    @FunctionalInterface private interface DeviceCall { Mono<?> invoke(Map<String, Object> values, ToolContext context); }
    @FunctionalInterface private interface DriverCall { Mono<?> invoke(Map<String, Object> values, ToolContext context); }

    private ReactiveAgenticTool commandToolTool(ToolDefinition definition, CommandCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try { return call.invoke(objectMapper.readValue(arguments, Map.class), context); }
                catch (RuntimeException error) { return Mono.error(error); }
            }
        };
    }

    private ReactiveAgenticTool eventToolTool(ToolDefinition definition, EventCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try { return call.invoke(objectMapper.readValue(arguments, Map.class), context); }
                catch (RuntimeException error) { return Mono.error(error); }
            }
        };
    }

    private ReactiveAgenticTool deviceToolTool(ToolDefinition definition, DeviceCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try { return call.invoke(objectMapper.readValue(arguments, Map.class), context); }
                catch (RuntimeException error) { return Mono.error(error); }
            }
        };
    }

    private ReactiveAgenticTool driverToolTool(ToolDefinition definition, DriverCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try { return call.invoke(objectMapper.readValue(arguments, Map.class), context); }
                catch (RuntimeException error) { return Mono.error(error); }
            }
        };
    }

    private ReactiveAgenticTool profileTool(ToolDefinition definition, ProfileCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool pointTool(ToolDefinition definition, PointCall call) {
        return new ReactiveAgenticTool() {
            @Override public ToolDefinition definition() { return definition; }
            @Override public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : value == null ? null : Long.valueOf(value.toString());
    }

    private static long longValue(Object value) {
        return number(value) == null ? 0L : number(value);
    }

    private static int intValue(Object value) {
        return number(value) == null ? 0 : number(value).intValue();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static java.util.List<Long> numbers(Object value) {
        if (!(value instanceof java.util.List<?> values)) return java.util.List.of();
        return values.stream().map(ReactiveAgenticToolRegistry::number)
                .filter(java.util.Objects::nonNull).toList();
    }
}
