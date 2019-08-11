package com.pnoker.device.group.receive.wia;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.device.group.bean.wia.MyGateway;
import com.pnoker.device.group.bean.wia.MyHartDevice;
import com.pnoker.device.group.bean.wia.MyHartVariable;
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
        List<MyGateway> myGatewayList = wiaGatewayList();
        log.info("gateWayList:{}", JSON.toJSONString(myGatewayList));
    }

    /**
     * 获取全部的WiaGateway数据
     *
     * @return
     */
    public List<MyGateway> wiaGatewayList() {
        List<MyGateway> myGatewayList = new ArrayList<>();

        QueryWrapper<WiaGateway> queryWrapper = new QueryWrapper<>();
        List<WiaGateway> list = wiaGatewayService.list(queryWrapper);
        list.forEach(wiaGateway -> myGatewayList.add(
                new MyGateway(wiaGateway.getId(), wiaGateway.getIpAddress(), wiaGateway.getLocalPort(), wiaGateway.getPort(), wiaDeviceList(wiaGateway.getId()))
        ));
        return myGatewayList;
    }

    /**
     * 根据WiaGatewayId获取网关下全部的设备数据
     *
     * @param gatewayId
     * @return
     */
    public List<MyHartDevice> wiaDeviceList(long gatewayId) {
        List<MyHartDevice> myHartDeviceList = new ArrayList<>();

        QueryWrapper<WiaDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("gateway_id", gatewayId);
        List<WiaDevice> list = wiaDeviceService.list(queryWrapper);
        list.forEach(wiaDevice -> myHartDeviceList.add(
                new MyHartDevice(wiaDevice.getId(), wiaDevice.getLongAddress(), wiaVariableList(wiaDevice.getId()))
        ));
        return myHartDeviceList;
    }

    /**
     * 根据WiaDeviceId获取设备下全部的变量数据
     *
     * @param deviceId
     * @return
     */
    public List<MyHartVariable> wiaVariableList(long deviceId) {
        List<MyHartVariable> myHartVariableList = new ArrayList<>();

        QueryWrapper<WiaVariable> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        List<WiaVariable> list = wiaVariableService.list(queryWrapper);
        list.forEach(wiaVariable -> myHartVariableList.add(
                new MyHartVariable(wiaVariable.getId(), wiaVariable.getStart(), wiaVariable.getEnd(), wiaVariable.getType())
        ));
        return myHartVariableList;
    }
}
