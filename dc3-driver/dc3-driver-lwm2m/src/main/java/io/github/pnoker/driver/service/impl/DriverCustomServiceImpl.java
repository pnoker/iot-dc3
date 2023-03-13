package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.sdk.service.DriverCustomService;
import io.github.pnoker.driver.server.Lwm2mServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

import static io.github.pnoker.common.sdk.utils.DriverUtil.attribute;


/**
 * @author xwh1998
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private Lwm2mServer lwm2mServer;


    @Override
    public void initial() {
        lwm2mServer.startServer();
    }

    @Override
    public void schedule() {
    }

    /**
     * 读取值 or 订阅
     *
     * @param driverInfo Driver Attribute Info
     * @param pointInfo  Point Attribute Info
     * @param device     Device
     * @param point      Point
     * @return
     */
    @Override
    public String read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, Point point) {
        //可以主动读取,也可以订阅资源
        return lwm2mServer.readValueByPath(device.getId(), attribute(pointInfo, "messageUp"));
    }

    /**
     * 写入值 or 执行函数
     *
     * @param driverInfo Driver Attribute Info
     * @param pointInfo  Point Attribute Info
     * @param device     Device
     * @param value      Value Attribute Info
     * @return
     */
    @Override
    public Boolean write(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo, Device device, AttributeInfo value) {


        return lwm2mServer.writeValueByPath(device.getId(), attribute(pointInfo, "messageDown"), value.getValue(), false);
    }
}
