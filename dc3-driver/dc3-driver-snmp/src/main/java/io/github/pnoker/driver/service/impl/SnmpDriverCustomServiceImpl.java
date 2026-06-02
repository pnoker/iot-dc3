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

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SNMP driver service implementation.
 * <p>
 * Communicates with SNMP-enabled devices using the SNMP4J library.
 * Supports SNMP v1 and v2c with GET/SET operations for reading and writing
 * OID values on remote devices.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class SnmpDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Snmp> clientMap;

    public SnmpDriverCustomServiceImpl(DriverMetadata driverMetadata,
                                       DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        clientMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // SNMP drivers do not need custom scheduled tasks.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        if (clientMap.containsKey(device.getId())) {
            return DeviceHealthState.online();
        }
        Snmp snmp = getConnector(device.getId(), driverConfig);
        return Objects.nonNull(snmp) ? DeviceHealthState.online() : DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                Snmp removed = clientMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    try {
                        removed.close();
                    } catch (IOException e) {
                        log.warn("SNMP client close failed, deviceId={}, message={}", metadataEvent.getId(), e.getMessage());
                    }
                    log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        Snmp snmp = getConnector(device.getId(), driverConfig);
        String oid = getConfigValue(pointConfig, "oid", "");
        try {
            CommunityTarget target = buildTarget(device.getId(), driverConfig);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);
            if (Objects.isNull(response) || Objects.isNull(response.getResponse())) {
                throw new ReadPointException("SNMP response is null, protocol={}, oid={}", driverCode, oid);
            }

            VariableBinding vb = response.getResponse().get(0);
            Variable variable = vb.getVariable();
            return new ReadPointValue(device, point, variable.toString());
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new ReadPointException("SNMP read failed, protocol={}, oid={}, message={}",
                    driverCode, oid, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        Snmp snmp = getConnector(device.getId(), driverConfig);
        String oid = getConfigValue(pointConfig, "oid", "");
        String snmpType = getConfigValue(pointConfig, "snmpType", "OCTET_STRING");
        try {
            CommunityTarget target = buildTarget(device.getId(), driverConfig);

            PDU pdu = new PDU();
            Variable variable = createVariable(snmpType, writePointValue.getValue(String.class));
            pdu.add(new VariableBinding(new OID(oid), variable));
            pdu.setType(PDU.SET);

            ResponseEvent response = snmp.send(pdu, target);
            if (Objects.isNull(response) || Objects.isNull(response.getResponse())) {
                throw new WritePointException("SNMP set response is null, protocol={}, oid={}", driverCode, oid);
            }
            return true;
        } catch (WritePointException e) {
            throw e;
        } catch (Exception e) {
            clientMap.remove(device.getId());
            throw new WritePointException("SNMP write failed, protocol={}, oid={}, message={}",
                    driverCode, oid, e.getMessage(), e);
        }
    }

    /**
     * Get or create an SNMP client for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration
     * @return cached or newly created Snmp instance
     */
    private Snmp getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return clientMap.computeIfAbsent(deviceId, id -> {
            try {
                TransportMapping<org.snmp4j.smi.UdpAddress> transport = new DefaultUdpTransportMapping();
                Snmp snmp = new Snmp(transport);
                transport.listen();
                log.info("Driver SNMP connection established, protocol={}, deviceId={}", driverCode, deviceId);
                return snmp;
            } catch (Exception e) {
                log.error("Driver SNMP connection failed, protocol={}, deviceId={}, message={}",
                        driverCode, deviceId, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Build an SNMP community target from driver configuration.
     *
     * @param deviceId     device identifier
     * @param driverConfig driver configuration
     * @return configured CommunityTarget
     */
    private CommunityTarget buildTarget(Long deviceId, Map<String, AttributeBO> driverConfig) {
        String host = getConfigValue(driverConfig, "host", "127.0.0.1");
        int port = getConfigIntValue(driverConfig, "port", 161);
        String version = getConfigValue(driverConfig, "version", "v2c");
        String community = getConfigValue(driverConfig, "community", "public");
        int timeout = getConfigIntValue(driverConfig, "timeout", 5000);
        int retries = getConfigIntValue(driverConfig, "retries", 1);

        Address address = GenericAddress.parse(String.format("udp:%s/%d", host, port));
        CommunityTarget target = new CommunityTarget();
        target.setAddress(address);
        target.setCommunity(new OctetString(community));
        target.setTimeout(timeout);
        target.setRetries(retries);

        if ("v1".equalsIgnoreCase(version)) {
            target.setVersion(SnmpConstants.version1);
        } else {
            target.setVersion(SnmpConstants.version2c);
        }
        return target;
    }

    /**
     * Create an SNMP variable of the specified type.
     *
     * @param snmpType the SNMP variable type name
     * @param value    the string value
     * @return the appropriate Variable implementation
     */
    private Variable createVariable(String snmpType, String value) {
        return switch (snmpType.toUpperCase()) {
            case "INTEGER", "INTEGER32" -> new org.snmp4j.smi.Integer32(Integer.parseInt(value));
            case "UNSIGNED_INTEGER32", "GAUGE32", "COUNTER32" -> new org.snmp4j.smi.Gauge32(Long.parseLong(value));
            case "COUNTER64" -> new org.snmp4j.smi.Counter64(Long.parseLong(value));
            case "TIMETICKS" -> new org.snmp4j.smi.TimeTicks(Long.parseLong(value));
            case "OID" -> new OID(value);
            case "IPADDRESS" -> new org.snmp4j.smi.IpAddress(value);
            case "NULL" -> new org.snmp4j.smi.Null();
            default -> new OctetString(value);
        };
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }
}
