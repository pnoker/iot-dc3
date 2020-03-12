package com.github.pnoker.driver.service.impl;

import com.github.pnoker.common.exception.ServiceException;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.model.Point;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverService;
import com.github.pnoker.common.sdk.service.rabbit.PointValueService;
import com.github.pnoker.driver.blocks.PlcDb;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.S7Serializer;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import com.github.s7connector.api.factory.S7SerializerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.pnoker.common.sdk.util.DriverUtils.attribute;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    @Resource
    private PointValueService pointValueService;
    @Resource
    private DriverContext driverContext;

    private volatile Map<Long, S7Connector> connectorMap;

    @Override
    public void initial() {
        connectorMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        S7Connector connector = getConnector(device.getId(), driverInfo);
        if (null != connector) {
            S7Serializer serializer = S7SerializerFactory.buildSerializer(connector);

            PlcDb plcDb = serializer.dispense(PlcDb.class, attribute(driverInfo, "dbNum"), attribute(driverInfo, "byteOffset"));
        }
        throw new ServiceException("invalid plcs7 connector");
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo value) {
        return false;
    }

    @Override
    public void schedule() {

    }

    /**
     * 获取 plcs7 connector
     * 先从缓存中取，没有就新建
     *
     * @param deviceId
     * @param driverInfo
     * @return
     */
    private S7Connector getConnector(Long deviceId, Map<String, AttributeInfo> driverInfo) {
        if (null == connectorMap.get(deviceId)) {
            String host = attribute(driverInfo, "host");
            Integer port = attribute(driverInfo, "port");
            try {
                S7Connector s7Connector = S7ConnectorFactory.buildTCPConnector().withHost(host).withPort(port).build();
                connectorMap.put(deviceId, s7Connector);
            } catch (Exception e) {
                log.error("new s7connector fail {}", e.getMessage(), e);
            }
        }
        return connectorMap.get(deviceId);
    }

}
