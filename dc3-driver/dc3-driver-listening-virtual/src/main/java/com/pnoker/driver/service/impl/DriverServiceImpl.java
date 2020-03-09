package com.pnoker.driver.service.impl;

import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.sdk.service.pool.ThreadPool;
import com.pnoker.common.sdk.service.rabbit.PointValueService;
import com.pnoker.driver.service.netty.NettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    @Value("${driver.custom.socket.dev-port}")
    private Integer devPort;
    @Value("${driver.custom.socket.pro-port}")
    private Integer proPort;
    @Resource
    private NettyServer nettyServer;
    @Resource
    private ThreadPool threadPool;

    @Override
    public void initial() {
        threadPool.execute(() -> {
            log.info("start socket:{}", devPort);
            nettyServer.start(devPort);
        });
        threadPool.execute(() -> {
            log.info("start socket:{}", proPort);
            nettyServer.start(proPort);
        });
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        return "";
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo value) {
        return false;
    }

    @Override
    public void schedule() {

    }

}
