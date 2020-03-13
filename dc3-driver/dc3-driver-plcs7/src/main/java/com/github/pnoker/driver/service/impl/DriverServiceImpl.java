package com.github.pnoker.driver.service.impl;

import com.github.pnoker.common.exception.ServiceException;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.model.Point;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverService;
import com.github.pnoker.common.sdk.service.rabbit.PointValueService;
import com.github.pnoker.driver.bean.PointVariable;
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

    /**
     * Plc Connector Map
     */
    private volatile Map<Long, S7Connector> s7ConnectorMap;

    @Override
    public void initial() {
        s7ConnectorMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        S7Serializer serializer = getS7Serializer(device.getId(), driverInfo);
        PointVariable pointVariable = getPointVariable(pointInfo);
        return String.valueOf(serializer.dispense(pointVariable));
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo value) {
        return false;
    }

    @Override
    public void schedule() {

    }

    /**
     * 获取 plcs7 serializer
     * 先从缓存中取，没有就新建
     *
     * @param deviceId
     * @param driverInfo
     * @return
     */
    private S7Serializer getS7Serializer(Long deviceId, Map<String, AttributeInfo> driverInfo) {
        S7Connector s7Connector = s7ConnectorMap.get(deviceId);
        if (null == s7Connector) {
            String host = attribute(driverInfo, "host");
            Integer port = attribute(driverInfo, "port");
            try {
                s7Connector = S7ConnectorFactory.buildTCPConnector().withHost(host).withPort(port).build();
            } catch (Exception e) {
                throw new ServiceException("new s7connector fail" + e.getMessage());
            }
        }
        if (null != s7Connector) {
            s7ConnectorMap.put(deviceId, s7Connector);
            return S7SerializerFactory.buildSerializer(s7Connector);
        }
        throw new ServiceException("new s7connector fail");
    }

    /**
     * 获取位号变量信息
     *
     * @param pointInfo
     * @return
     */
    private PointVariable getPointVariable(Map<String, AttributeInfo> pointInfo) {
        int dbNum = attribute(pointInfo, "dbNum");
        int byteOffset = attribute(pointInfo, "byteOffset");
        int bitOffset = attribute(pointInfo, "bitOffset");
        int blockSize = attribute(pointInfo, "blockSize");
        String type = attribute(pointInfo, "type");
        return new PointVariable(dbNum, byteOffset, bitOffset, blockSize, type);
    }

}
