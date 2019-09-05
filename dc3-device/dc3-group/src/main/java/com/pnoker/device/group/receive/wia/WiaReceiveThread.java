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

import com.pnoker.device.group.bean.wia.MyGateway;
import com.pnoker.device.group.util.DatagramUtils;
import com.pnoker.device.group.util.PackageProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Data
@Slf4j
public class WiaReceiveThread implements Runnable {
    private MyGateway myGateway;

    public WiaReceiveThread(MyGateway myGateway) {
        this.myGateway = myGateway;
    }

    @Override
    public void run() {
        sendDatagram();
        try {
            myGateway.getDatagramSocket().receive(myGateway.getDatagramReceive());
        } catch (IOException e) {
            log.error("receive datagram timeout", e);
        }
        byte[] receive = myGateway.getDatagramReceive().getData();
        PackageProcessor processor = new PackageProcessor(receive);
        String hexDatagram = DatagramUtils.hexDatagram(receive, receive.length);
        String dataHead = processor.bytesToString(0, 1);

        switch (dataHead) {
            case "0101":
                log.info("网络报文:{}", hexDatagram);
                break;
            case "0111":
                log.info("统计报文:{}", hexDatagram);
                break;
            case "010f":
                log.info("健康报文:{}", hexDatagram);
                break;
            case "0183":
                log.info("数据报文:{}", hexDatagram);
                break;
            default:
                log.info("其他报文:{}", hexDatagram);
                break;
        }
    }

    public void sendDatagram() {
        try {
            myGateway.getDatagramSocket().send(myGateway.getDatagramSend());
        } catch (IOException e) {
            log.error("send datagram fail", e);
        }
    }
}
