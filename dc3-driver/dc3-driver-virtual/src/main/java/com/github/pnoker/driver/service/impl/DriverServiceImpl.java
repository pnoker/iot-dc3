package com.github.pnoker.driver.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.model.Point;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverService;
import com.github.pnoker.common.sdk.service.rabbit.PointValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

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

    @Override
    public void initial() {
        log.info("virtual driver");
    }

    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        String host = attribute(driverInfo, "host");
        Integer port = attribute(driverInfo, "port");
        String tag = attribute(pointInfo, "tag");
        String value = String.valueOf(RandomUtil.randomDouble(100));
        log.info("driverInfo:{},{},pointInfo:{},device:{}.{},point:{}.{},value:{}", host, port, tag, device.getId(), device.getName(), point.getId(), point.getName(), value);
        return value;
    }

    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, AttributeInfo value) {
        return false;
    }

    @Override
    public void schedule() {

    }

}
