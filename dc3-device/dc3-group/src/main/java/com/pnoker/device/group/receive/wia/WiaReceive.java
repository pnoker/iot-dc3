package com.pnoker.device.group.receive.wia;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.device.group.bean.wia.MyHartDevice;
import com.pnoker.device.group.model.wia.WiaDevice;
import com.pnoker.device.group.model.wia.WiaGateway;
import com.pnoker.device.group.model.wia.WiaVariable;
import com.pnoker.device.group.service.wia.WiaDeviceService;
import com.pnoker.device.group.service.wia.WiaGatewayService;
import com.pnoker.device.group.service.wia.WiaVariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Order(1)
@Component
public class WiaReceive implements ApplicationRunner {
    @Autowired
    private WiaGatewayService wiaGatewayService;
    @Autowired
    private WiaDeviceService wiaDeviceService;
    @Autowired
    private WiaVariableService wiaVariableService;

    @Override
    public void run(ApplicationArguments args) {
    }

    public List<WiaGateway> wiaGatewayList() {
        QueryWrapper<WiaGateway> queryWrapper = new QueryWrapper<>();
        List<WiaGateway> list = wiaGatewayService.list(queryWrapper);
        return list;
    }

    public List<WiaDevice> wiaDeviceList(int gatewayId) {
        List<MyHartDevice> myHartDeviceList = new ArrayList<>();
        QueryWrapper<WiaDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("gateway_id", gatewayId);
        List<WiaDevice> list = wiaDeviceService.list(queryWrapper);
        list.forEach(wiaDevice -> {
            //MyHartDevice myHartDevice = new MyHartDevice(wiaDevice.getId(),wiaDevice.getName());
        });
        return list;
    }

    public List<WiaVariable> wiaVariableList(int deviceId) {
        QueryWrapper<WiaVariable> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        List<WiaVariable> list = wiaVariableService.list(queryWrapper);
        return list;
    }
}
