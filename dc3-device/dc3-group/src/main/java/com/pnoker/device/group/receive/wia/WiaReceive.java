/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.device.group.receive.wia;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.device.group.bean.wia.MyGateway;
import com.pnoker.device.group.bean.wia.MyHartDevice;
import com.pnoker.device.group.bean.wia.MyHartVariable;
import com.pnoker.device.group.constant.Queues;
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
        myGatewayList.forEach(myGateway -> {
            WiaReceiveThread receiveThread = new WiaReceiveThread(myGateway);
            Queues.receivePoolExecutor.execute(receiveThread);
        });
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
                new MyHartVariable(wiaVariable.getId(), wiaVariable.getStartIndex(), wiaVariable.getEndIndex(), wiaVariable.getParseType())
        ));
        return myHartVariableList;
    }
}
