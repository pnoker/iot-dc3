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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SL651-2014 hydrological telemetry driver service.
 * <p>
 * SL651 is a server-side protocol where remote hydrological monitoring
 * stations push telemetry data to a central server. This driver starts an
 * SL651 TCP server and listens for incoming station reports. Received data
 * is parsed and forwarded to the DC3 platform.
 * </p>
 * <p>
 * Unlike PLC drivers that actively connect to devices and read/write points,
 * SL651 data arrives asynchronously and unsolicited from remote stations.
 * The {@code read} and {@code write} methods are not used in this driver.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Sl651DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final DeviceMetadata deviceMetadata;
    private final PointMetadata pointMetadata;

    @Value("${dc3.driver.code}")
    private String driverCode;

    @Value("${dc3.driver.sl651.port:5001}")
    private int serverPort;

    @Value("${dc3.driver.sl651.pwd:0000}")
    private String serverPwd;

    private Object sl651Server;

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
        }
    }

    @Override
    public void initial() {
        log.info("Driver initializing, protocol={}", driverCode);
        startServer();
    }

    @Override
    public void schedule() {
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();

        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)) {
                stopServer();
                log.info("Driver server stopped due to device delete, protocol={}, deviceId={}",
                        driverCode, metadataEvent.getId());
            }
            if (MetadataOperateTypeEnum.ADD.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                restartServer();
                log.info("Driver server restarted due to device change, protocol={}, deviceId={}",
                        driverCode, metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    // ------------------------------------------------------------------------
    //  server lifecycle
    // ------------------------------------------------------------------------

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        return null;
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        return false;
    }

    private void restartServer() {
        stopServer();
        startServer();
    }

    // ------------------------------------------------------------------------
    //  message listener
    // ------------------------------------------------------------------------

    private void startServer() {
        try {
            Class<?> serverClass = Class.forName("com.github.xingshuangs.iot.protocol.sl651.service.SL651Server");
            Class<?> listenerClass = Class.forName("com.github.xingshuangs.iot.protocol.sl651.event.ISl651MessageListener");
            Object server = serverClass.getConstructor(String.class).newInstance(serverPwd);
            Object listener = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if ("onMessage".equals(method.getName()) && Objects.nonNull(args) && args.length == 3) {
                            byte[] messageBytes = args[0] instanceof byte[] bytes ? bytes : new byte[0];
                            handleSl651Message(messageBytes, args[1], args[2]);
                        }
                        return null;
                    });
            serverClass.getMethod("setMessageListener", listenerClass).invoke(server, listener);
            serverClass.getMethod("start", int.class).invoke(server, serverPort);
            sl651Server = server;
            log.info("Driver SL651 server started, protocol={}, port={}", driverCode, serverPort);
        } catch (ClassNotFoundException e) {
            log.warn("Driver SL651 server unavailable, protocol={}, reason=sl651ApiMissing", driverCode, e);
        } catch (Exception e) {
            log.error("Driver SL651 server start failed, protocol={}, port={}", driverCode, serverPort, e);
        }
    }

    private void stopServer() {
        Object server = sl651Server;
        if (Objects.isNull(server)) {
            return;
        }
        try {
            Method isAlive = server.getClass().getMethod("isAlive");
            boolean alive = Boolean.TRUE.equals(isAlive.invoke(server));
            if (alive) {
                server.getClass().getMethod("stop").invoke(server);
                log.info("Driver SL651 server stopped, protocol={}", driverCode);
            }
        } catch (Exception e) {
            log.error("Driver SL651 server stop failed, protocol={}", driverCode, e);
        } finally {
            sl651Server = null;
        }
    }

    void forwardTelemetry(String stationAddr, List<String> elements) {
        if (Objects.isNull(elements) || elements.isEmpty()) {
            return;
        }

        List<PointValue> pointValues = new ArrayList<>(16);
        Set<Long> deviceIds = driverMetadata.getDeviceIds();
        if (Objects.isNull(deviceIds) || deviceIds.isEmpty()) {
            return;
        }
        for (Long deviceId : deviceIds) {
            DeviceBO device = deviceMetadata.getCache(deviceId);
            if (!matchesStation(stationAddr, device)) {
                continue;
            }

            Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointConfig(deviceId);
            if (Objects.isNull(pointConfigMap) || pointConfigMap.isEmpty()) {
                continue;
            }

            for (Map.Entry<Long, Map<String, AttributeBO>> entry : pointConfigMap.entrySet()) {
                PointBO point = pointMetadata.getCache(entry.getKey());
                Integer index = getElementIndex(entry.getValue());
                if (Objects.isNull(point) || Objects.isNull(index) || index < 0 || index >= elements.size()) {
                    continue;
                }
                pointValues.add(new PointValue(new ReadPointValue(device, point, elements.get(index))));
            }
        }

        if (!pointValues.isEmpty()) {
            driverSenderService.pointValueSender(pointValues);
            log.debug("Driver SL651 point values forwarded, protocol={}, stationAddr={}, count={}",
                    driverCode, stationAddr, pointValues.size());
        }
    }

    private boolean matchesStation(String stationAddr, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(stationAddr)) {
            return false;
        }
        return stationAddr.equalsIgnoreCase(device.getDeviceCode())
                || stationAddr.equalsIgnoreCase(device.getDeviceName());
    }

    private Integer getElementIndex(Map<String, AttributeBO> pointConfig) {
        if (Objects.isNull(pointConfig) || Objects.isNull(pointConfig.get("index"))) {
            return null;
        }
        try {
            return pointConfig.get("index").getValue(Integer.class);
        } catch (Exception e) {
            log.warn("Driver SL651 point config skipped, protocol={}, reason=invalidIndex", driverCode, e);
            return null;
        }
    }

    private void handleSl651Message(byte[] bytes, Object response, Object bodyResponses) {
        String stationAddr = bytesToHex(invokeBytes(response, "getRemoteStationAddress"));
        String funcCode = bytesToHex(invokeBytes(response, "getFunctionCode"));
        List<String> elements = extractBodyElements(bodyResponses);
        log.debug("Driver SL651 message received, protocol={}, stationAddr={}, funcCode={}, bodyCount={}",
                driverCode, stationAddr, funcCode, elements.size());
        forwardTelemetry(stationAddr, elements);
    }

    private byte[] invokeBytes(Object target, String methodName) {
        if (Objects.isNull(target)) {
            return new byte[0];
        }
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof byte[] bytes ? bytes : new byte[0];
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("Driver SL651 response field unavailable, protocol={}, method={}", driverCode, methodName, e);
            return new byte[0];
        }
    }

    private List<String> extractBodyElements(Object bodyResponses) {
        if (!(bodyResponses instanceof List<?> responses) || responses.isEmpty()) {
            return List.of();
        }

        List<String> elements = new ArrayList<>(16);
        for (int i = 0; i < responses.size(); i++) {
            Object body = responses.get(i);
            List<String> bodyElements = invokeBodyElements(body);
            if (!bodyElements.isEmpty()) {
                log.debug("Driver SL651 body[{}], protocol={}, elements={}", i, driverCode, bodyElements);
                elements.addAll(bodyElements);
            }
        }
        return elements;
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeBodyElements(Object body) {
        if (Objects.isNull(body)) {
            return List.of();
        }
        try {
            Object value = body.getClass().getMethod("getBodyElements").invoke(body);
            return value instanceof List<?> ? (List<String>) value : List.of();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("Driver SL651 body elements unavailable, protocol={}", driverCode, e);
            return List.of();
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "port", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "index", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}